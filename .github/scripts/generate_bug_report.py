#!/usr/bin/env python3
"""
generate_bug_report.py — customer-facing PDF defect report, one formal bug entry
per FINAL-failed test (post-rerun), with steps-to-reproduce and screenshots.

For every failed test the report contains:
    Title            [Feature Area] short defect statement
    Environment      QA / iOS Simulator / app version (read from the committed
                     .app Info.plist) / device / CI run link
    Preconditions    module-aware (logged in, site selected, ...)
    Steps to Reproduce  mined from the Detailed ExtentReport step log (logStep
                     rows), falling back to logStep literals in the Java test
                     source, falling back to a module template
    Actual Result    assertion failure text (Expected/Actual parsed) + exception
    Expected Result  parsed from the assertion or the test's description
    Severity/Priority  keyword heuristics + optional overrides JSON
    Attachments      failure screenshot (screenshots/<method>_FAILED_*.png) and
                     the last in-report step screenshot before the failure

Data sources (all already present in the send-email job's all-reports/ download):
  <artifact>/target/surefire-reports/testng-results.xml   status + <exception>
  <artifact>/reports/detailed/Detailed_Report_*.html      step rows + base64 shots
  <artifact>/screenshots/<method>_FAILED_<ts>.png         full-size failure PNG

Merge semantics mirror ios_client_report.py exactly: rerun outcome overrides the
primary per (class, method) EXCEPT a rerun SKIP never overrides a non-SKIP
original (run 28666174784: 285 FAILs must never become SKIPs). Modules whose CI
key contains --rerun-key (default 'failures-rerun') are treated as the rerun set.

Usage (CI, from repo root, after downloading all artifacts into all-reports/):
  python3 .github/scripts/generate_bug_report.py \
      --results all-reports --out reports/Bug_Report.pdf \
      --run-url "$RUN_URL" --run-id "$GITHUB_RUN_ID"

Local self-test (no artifacts needed):  --selftest
"""

import argparse
import base64
import glob
import html as html_mod
import io
import json
import os
import re
import sys
from collections import defaultdict
from datetime import datetime, timezone

import subprocess

try:
    import defusedxml.ElementTree as ET  # XXE-safe
except ImportError:  # pragma: no cover
    subprocess.run([sys.executable, "-m", "pip", "install", "--quiet",
                    "--break-system-packages", "defusedxml"],
                   check=False)
    try:
        import defusedxml.ElementTree as ET
    except ImportError:
        subprocess.run([sys.executable, "-m", "pip", "install", "--quiet", "defusedxml"])
        import defusedxml.ElementTree as ET


# ── CI module key → client-facing feature area (mirror of ios_client_report.py) ──
AREA_RULES = [
    ("auth", "User Login & Access"),
    ("smoke", "Smoke / Core CRUD"),
    ("sitevisit", "Site Visits & Work Orders"),  # must precede the 'site' prefix rule
    ("site-selection", "Site & Facility Selection"),
    ("site", "Site & Facility Selection"),
    ("connections", "Electrical Connections"),
    ("connection", "Electrical Connections"),
    ("location", "Locations (Buildings, Floors, Rooms)"),
    ("issues", "Issue Tracking"),
    ("issue", "Issue Tracking"),
    ("assets", "Asset Management"),
    ("asset-engineer", "Asset Engineering (Equipment Library)"),
    ("asset", "Asset Management"),
    ("arc-flash", "Arc Flash"),
    ("offline", "Offline & Sync"),
    ("workorder", "Work Order Planning"),
    ("worktype", "Work Types"),
    ("zp323", "ZP-323 New Features"),
    ("api-contract", "API Contract & Data Integrity"),
    ("api", "API Contract & Data Integrity"),
    ("s3-drift", "Infrastructure (S3 Policy)"),
    ("s3", "Infrastructure (S3 Policy)"),
    ("security", "Security & Edge Cases"),
]

# Class-name fallback when the module key is uninformative (e.g. rerun shards
# mix every module into one artifact).
CLASS_AREA_RULES = [
    ("ArcFlash", "Arc Flash"),
    ("AssetEngineer", "Asset Engineering (Equipment Library)"),
    ("Asset_", "Asset Management"),
    ("Authentication", "User Login & Access"),
    ("Connections", "Electrical Connections"),
    ("Issue_", "Issue Tracking"),
    ("Location", "Locations (Buildings, Floors, Rooms)"),
    ("Offline", "Offline & Sync"),
    ("Security", "Security & Edge Cases"),
    ("SiteSelection", "Site & Facility Selection"),
    ("SiteVisit", "Site Visits & Work Orders"),
    ("WorkOrder", "Work Order Planning"),
    ("WorkType", "Work Types"),
    ("ZP323", "ZP-323 New Features"),
    ("E2E_", "End-to-End Integrity"),
    ("S3Bucket", "Infrastructure (S3 Policy)"),
    ("Settings", "App Settings"),
]


def area_for(module_key: str, cls: str = "") -> str:
    key = (module_key or "").lower()
    for prefix, area in AREA_RULES:
        if key.startswith(prefix) or prefix in key:
            return area
    short = cls.rsplit(".", 1)[-1]
    for prefix, area in CLASS_AREA_RULES:
        if short.startswith(prefix) or prefix in short:
            return area
    return (module_key or short or "General").replace("-", " ").replace("_", " ").title()


def module_from_path(xml_path: str) -> str:
    parts = xml_path.replace("\\", "/").split("/")
    for p in parts:
        if p.endswith("-report"):
            return p[: -len("-report")]
    if "target" in parts:
        i = parts.index("target")
        if i > 0:
            return parts[i - 1]
    return os.path.basename(os.path.dirname(xml_path)) or "unknown"


def artifact_root_of(xml_path: str) -> str:
    """<root>/target/surefire-reports/testng-results.xml → <root>."""
    d = os.path.dirname(os.path.abspath(xml_path))          # surefire-reports
    if os.path.basename(d) == "surefire-reports":
        d = os.path.dirname(d)                               # target
    if os.path.basename(d) == "target":
        d = os.path.dirname(d)                               # artifact root
    return d


# ── TestNG XML parsing (extended: exception + evidence roots) ────────────────

_UNSTABLE_PARAM = re.compile(r"^\[L[\w.$]+;@[0-9a-f]+$")


def param_signature(tm) -> str:
    """Stable identity for one data-provider invocation.

    TestNG writes <params><parameter index=N><value>…</value></parameter></params>.
    Array/object params render as `[Ljava.lang.String;@1f010bf0` — an identity hash
    that changes every JVM, so those are dropped; the scalar values that remain
    ('Fuse', 'Circuit Breaker', '..9') identify the case across runs.
    Retries of the same invocation carry identical params, so retry collapse keeps
    working per invocation.
    """
    params = tm.find("params")
    if params is None:
        return ""
    vals = []
    for v in params.iter("value"):
        t = (v.text or "").strip()
        if not t or _UNSTABLE_PARAM.match(t):
            continue
        vals.append(t[:60])
    return " | ".join(vals[:3])


def device_class_of(module: str) -> str:
    """iPad and iPhone jobs run the SAME suite — a failure on one device must not
    be laundered by a pass on the other, so device is part of the identity."""
    return "ipad" if "ipad" in (module or "").lower() else "phone"


def parse_methods(results_dir: str, rerun_key: str, exclude: set):
    """Return (primary, rerun) lists of per-invocation dicts.

    Dict: {module, cls, method, params, status, ms, exc_class, exc_msg, exc_stack,
           roots: [artifact roots this test was seen in, FAIL-bearing first]}
    Identity = (class, method, param-signature, device class); retries collapse to
    PASS if ever passed, else FAIL, else SKIP. Keying by invocation matters: e.g.
    TC_ENG_050 fails for 4 of 38 asset classes — method-level collapse would report
    it as a pass and lose 4 real defects.
    """
    buckets = {"primary": defaultdict(dict), "rerun": defaultdict(dict)}
    pattern = os.path.join(results_dir, "**", "testng-results.xml")
    files = sorted(glob.glob(pattern, recursive=True))
    if not files:
        print(f"  ! no testng-results.xml under {results_dir}", file=sys.stderr)
    for xml_path in files:
        module = module_from_path(xml_path)
        if any(ex and ex in module for ex in exclude):
            continue
        kind = "rerun" if rerun_key and rerun_key in module else "primary"
        root_dir = artifact_root_of(xml_path)
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
                name, status = tm.get("name"), tm.get("status")
                if not name or not status:
                    continue
                psig = param_signature(tm)
                key = (fqcn, name, psig, device_class_of(module))
                rec = buckets[kind].setdefault(key, {
                    "module": module, "cls": fqcn, "method": name, "params": psig,
                    "statuses": set(), "ms": 0.0,
                    "exc_class": "", "exc_msg": "", "exc_stack": "",
                    "fail_roots": [], "roots": [],
                })
                rec["statuses"].add(status)
                try:
                    rec["ms"] = max(rec["ms"], float(tm.get("duration-ms", 0)))
                except (TypeError, ValueError):
                    pass
                if root_dir not in rec["roots"]:
                    rec["roots"].append(root_dir)
                if status == "FAIL":
                    if root_dir not in rec["fail_roots"]:
                        rec["fail_roots"].append(root_dir)
                    exc = tm.find("exception")
                    if exc is not None and not rec["exc_msg"]:
                        rec["exc_class"] = (exc.get("class") or "").rsplit(".", 1)[-1]
                        msg_el = exc.find("message")
                        stk_el = exc.find("full-stacktrace")
                        rec["exc_msg"] = (msg_el.text or "").strip() if msg_el is not None else ""
                        stack = (stk_el.text or "").strip() if stk_el is not None else ""
                        rec["exc_stack"] = "\n".join(stack.splitlines()[:6])

    def collapse(bucket):
        out = []
        for _key, rec in bucket.items():
            sts = rec.pop("statuses")
            rec["status"] = "PASS" if "PASS" in sts else ("FAIL" if "FAIL" in sts else "SKIP")
            out.append(rec)
        return out

    return collapse(buckets["primary"]), collapse(buckets["rerun"])


def _merge_key(m):
    return (m["cls"], m["method"], m.get("params", ""), device_class_of(m["module"]))


def merge_rerun(primary, rerun):
    """ios_client_report.py semantics + evidence-root selection.

    Final-FAIL evidence prefers the rerun artifact (deterministic, fresh-sim,
    login-first) and falls back to the primary module artifact. Rerun rows are
    matched per invocation, then (for shards that produced no params) per method,
    so a data-driven case is never overridden by an unrelated invocation.
    """
    rr = {_merge_key(m): m for m in rerun}
    rr_by_method = {}
    for m in rerun:
        rr_by_method.setdefault((m["cls"], m["method"]), []).append(m)
    merged = []
    for m in primary:
        key = _merge_key(m)
        r = rr.get(key)
        if r is None:
            same = rr_by_method.get((m["cls"], m["method"]), [])
            # Only fall back to method-level matching when the rerun ran this test
            # exactly once and carried no parameters (i.e. it is unambiguous).
            if len(same) == 1 and not same[0].get("params"):
                r = same[0]
        if r and not (r["status"] == "SKIP" and m["status"] != "SKIP"):
            new = dict(m)
            new["status"] = r["status"]
            new["ms"] = r["ms"] or m["ms"]
            new["reran"] = True
            new["recovered"] = (m["status"] != "PASS" and r["status"] == "PASS")
            if r["status"] == "FAIL":
                new["exc_class"] = r["exc_class"] or m["exc_class"]
                new["exc_msg"] = r["exc_msg"] or m["exc_msg"]
                new["exc_stack"] = r["exc_stack"] or m["exc_stack"]
                new["evidence_roots"] = r["fail_roots"] + m["fail_roots"] + m["roots"]
            else:
                new["evidence_roots"] = m["roots"]
            merged.append(new)
        else:
            new = dict(m)
            new["reran"] = False
            new["recovered"] = False
            new["evidence_roots"] = m["fail_roots"] + m["roots"]
            merged.append(new)
    # rerun-only rows (e.g. rerun-failed-by-date.yml runs the rerun alone)
    seen = {_merge_key(m) for m in primary}
    for r in rerun:
        if _merge_key(r) not in seen:
            new = dict(r)
            new["reran"] = False
            new["recovered"] = False
            new["evidence_roots"] = r["fail_roots"] + r["roots"]
            merged.append(new)
    return merged


# ── Detailed ExtentReport HTML mining ─────────────────────────────────────────
#
# DOM contract (ExtentReportManager custom Spark render):
#   <li class="test-item" status="fail" ...>
#     <p class="name">TC_ISS_142 - Verify creating Repair Needed issue ...</p>
#     ... <tr class="event-row">
#           <td><span class="badge log info-bg">Info</span></td>
#           <td>4:40:13 PM</td>
#           <td>Step 1: ... <div class="eg-shot"...><img src="data:image/..."></div></td>
#         </tr>

_RE_NAME = re.compile(r'<p class="name">(.*?)</p>', re.S)
_RE_STATUS = re.compile(r'^\s*status="(\w+)"')
_RE_BADGE = re.compile(r'<span class="badge log (\w+)-bg">\s*(\w+)\s*</span>')
_RE_IMG = re.compile(r'src="(data:image/[^"]+)"')
_RE_TAG = re.compile(r"<[^>]+>")
_RE_TD = re.compile(r"<td[^>]*>(.*?)</td>", re.S)


def _strip_html(fragment: str) -> str:
    txt = _RE_TAG.sub(" ", fragment)
    txt = html_mod.unescape(txt)
    return re.sub(r"\s+", " ", txt).strip()


class DetailedReport:
    """One parsed Detailed_Report_*.html: TC-id → list of test blocks."""

    def __init__(self, path: str):
        self.path = path
        self.blocks = []          # [{status, name, rows:[(badge, text, [imgs])]}]
        try:
            raw = open(path, encoding="utf-8", errors="replace").read()
        except OSError:
            return
        for chunk in raw.split('<li class="test-item"')[1:]:
            status_m = _RE_STATUS.search(chunk)
            name_m = _RE_NAME.search(chunk)
            if not name_m:
                continue
            rows = []
            for row_html in chunk.split('<tr class="event-row">')[1:]:
                row_html = row_html.split("</tr>")[0]
                badge_m = _RE_BADGE.search(row_html)
                badge = badge_m.group(2) if badge_m else "Info"
                imgs = _RE_IMG.findall(row_html)
                tds = _RE_TD.findall(row_html)
                detail_html = tds[-1] if tds else row_html
                detail_html = re.sub(r'<div class="eg-shot".*', "", detail_html, flags=re.S)
                rows.append((badge, _strip_html(detail_html), imgs))
            self.blocks.append({
                "status": (status_m.group(1) if status_m else "").lower(),
                "name": _strip_html(name_m.group(1)),
                "rows": rows,
            })

    def find(self, method: str):
        """Best block for a test method: prefer failed blocks; prefer the last."""
        matches = [b for b in self.blocks if _name_matches_method(b["name"], method)]
        if not matches:
            return None
        failed = [b for b in matches if b["status"] in ("fail", "warning")]
        return (failed or matches)[-1]


def _name_matches_method(block_name: str, method: str) -> bool:
    """'TC_ENG_170 [Manufacturer] - desc' matches 'TC_ENG_170_enabledSave...'."""
    base = block_name.split(" - ")[0].split(" [")[0].strip()
    if not base:
        return False
    key = base.replace(" ", "_")
    if method == key:
        return True
    return method.startswith(key) and (len(method) == len(key) or method[len(key)] == "_")


_report_cache = {}


def detailed_reports_for(root_dir: str):
    if root_dir in _report_cache:
        return _report_cache[root_dir]
    paths = sorted(glob.glob(os.path.join(root_dir, "reports", "detailed", "Detailed_Report_*.html")),
                   reverse=True)
    reports = [DetailedReport(p) for p in paths]
    _report_cache[root_dir] = reports
    return reports


def failure_png_for(root_dir: str, method: str):
    """screenshots/<sanitized-method>_FAILED_<ts>.png — latest one."""
    safe = re.sub(r"[^a-zA-Z0-9_\-]", "_", method)
    hits = sorted(glob.glob(os.path.join(root_dir, "screenshots", safe + "_FAILED_*.png")))
    return hits[-1] if hits else None


# ── Steps-to-reproduce composition ────────────────────────────────────────────

_EMOJI_PREFIX = re.compile(r"^[\W_]*(?:📝|📸|⚡|⏭️|✅|❌|⚠️|ℹ️|🔁|🧹|🚨)\s*")
_STEP_PREFIX = re.compile(r"^step\s*\d+\s*[:.\-]\s*", re.I)
_ASSERT_PREFIX = re.compile(r"^assertion\s+(?:failed|passed)\s*[:\-]\s*", re.I)
# "IsAssetSavedAfterEdit: ..." / "SubmitCompanyCode: ..." — internal helper names
_IDENT_PREFIX = re.compile(r"^[A-Za-z][A-Za-z0-9_]{2,40}:\s+(?=\S)")
_TIMEOUT_MSG = re.compile(r"Method\s+\S+\(\)\s+didn't finish within the time-out\s*(\d+)?", re.I)
_EXC_ONLY = re.compile(r"^(?:java|javax|org|com|io)\.[\w.$]*(?:Exception|Error)$")

# Internal automation diagnostics that must never reach a customer-facing step list.
_INTERNAL_NOISE = ("wda", "appium", "page source", "xcuitest", "driver", "session is wedged",
                   "session dead", "circuit breaker", "implicit wait", "coordinate fallback",
                   "soft-restart", "stale element", "snapshot", "predicate", "locator")


def _clean_step(text: str) -> str:
    t = _EMOJI_PREFIX.sub("", text).strip()
    t = _STEP_PREFIX.sub("", t).strip()
    t = _ASSERT_PREFIX.sub("", t).strip()
    return t


# An Info row that is really a failure: "❌ …" / "Assertion failed: …".
_FAILED_ROW = re.compile(r"^\W*(?:❌|assertion\s+failed\b)", re.I)
# Pure diagnostic echoes — "Chapter options visible: false", "count: 0", "x: ''".
_DIAG_ECHO = re.compile(r"^.{1,70}:\s*(?:true|false|-?\d+(?:\.\d+)?|''|\"\"|\(none\)|null)$", re.I)


def _is_internal_noise(text: str) -> bool:
    low = text.lower()
    return any(k in low for k in _INTERNAL_NOISE)


def _is_diagnostic_echo(text: str, block_name: str) -> bool:
    """Value dumps and TC-id self-echoes are engineering trace, not user steps."""
    if _DIAG_ECHO.match(text):
        return True
    if len(text) < 3 or not re.search(r"[A-Za-z]", text):
        return True
    tcid = block_name.split(" - ")[0].split(" [")[0].strip()
    return bool(tcid) and text.startswith(tcid + ":")


def steps_from_block(block):
    """Return (steps, fail_texts, shots) from a detailed-report block.

    shots = [{'uri', 'caption', 'idx', 'role'}] in report order, where role is
    'context' | 'action' | 'failure' — the narrative the PDF needs to show a
    customer what happened before and at the moment of failure.
    """
    steps, fails = [], []
    img_rows = []                      # (row_idx, datauri, caption)
    first_fail_idx = None
    for idx, (badge, text, imgs) in enumerate(block["rows"]):
        caption = _clean_step(text)
        for u in imgs:
            img_rows.append((idx, u, caption))
        # A failure is logged TWICE: first as an Info row "❌ Assertion failed: …"
        # (screenshotOnAssertionFail — this is the row that carries the
        # moment-of-failure screenshot), then as the Fail-badged row. Both are
        # failure text, never reproduction steps.
        is_fail_row = badge.lower() == "fail" or _FAILED_ROW.match(text or "")
        if is_fail_row:
            if first_fail_idx is None:
                first_fail_idx = idx
            t = _clean_step(text)
            if t and t not in fails:
                fails.append(t)
            continue
        t = _clean_step(text)
        if not t or t.lower().startswith(("initial state", "final state")):
            continue
        if _is_internal_noise(t) or _is_diagnostic_echo(t, block["name"]):
            continue
        # logStep echoes assertion passes ("✅ X") right after "Step n: Verify X";
        # fold those duplicates away (but never drop the FIRST step: an empty
        # `prev` makes the substring test vacuously true).
        if text.strip().startswith("✅"):
            if steps:
                prev = steps[-1].lower()
                if t.lower() in prev or prev in t.lower():
                    continue
            t = "Verify: " + t
        if steps and steps[-1] == t:
            continue
        steps.append(t)
    # Build the visual narrative: opening state → the action that preceded the
    # failure → the failure moment. Captions carry the step each shot belongs to.
    shots = []
    if first_fail_idx is not None:
        before = [(i, u, c) for i, u, c in img_rows if i < first_fail_idx]
        at_after = [(i, u, c) for i, u, c in img_rows if i >= first_fail_idx]
        if before:
            shots.append({"uri": before[0][1], "idx": before[0][0], "role": "context",
                          "caption": before[0][2] or "Starting state"})
        if len(before) > 2:
            mid = before[len(before) // 2]
            shots.append({"uri": mid[1], "idx": mid[0], "role": "context",
                          "caption": mid[2] or "During the flow"})
        if len(before) > 1:
            shots.append({"uri": before[-1][1], "idx": before[-1][0], "role": "action",
                          "caption": before[-1][2] or "Last step before the failure"})
        if at_after:
            shots.append({"uri": at_after[0][1], "idx": at_after[0][0], "role": "failure",
                          "caption": "At the moment of failure"})
    else:
        for i, u, c in img_rows[:2]:
            shots.append({"uri": u, "idx": i, "role": "context",
                          "caption": c or "Captured screen state"})
        if img_rows:
            shots.append({"uri": img_rows[-1][1], "idx": img_rows[-1][0], "role": "failure",
                          "caption": "Final captured screen state"})
    return steps, fails, shots


_STR_LIT = re.compile(r'"((?:[^"\\]|\\.)*)"')


def _iter_call_args(body: str, open_pattern: str):
    """Yield the argument text of each `<call>(...)`, honouring nested parens and
    string literals. A `[^;]` regex would stop at a semicolon inside a string
    (e.g. logStep("Voltage: 480V; phase A")) and lose the step."""
    for m in re.finditer(open_pattern, body):
        i = m.end()
        depth, in_str, esc_next, start = 1, False, False, i
        while i < len(body) and depth:
            c = body[i]
            if esc_next:
                esc_next = False
            elif c == "\\":
                esc_next = True
            elif c == '"':
                in_str = not in_str
            elif not in_str:
                if c == "(":
                    depth += 1
                elif c == ")":
                    depth -= 1
            i += 1
        if depth == 0:
            yield body[start:i - 1]


def steps_from_source(src_root: str, fqcn: str, method: str) -> (list, str):
    """Fallback: logStep(...) literals inside the test method body + description."""
    if not src_root:
        return [], ""
    rel = fqcn.replace(".", os.sep) + ".java"
    path = os.path.join(src_root, rel)
    if not os.path.isfile(path):
        hits = glob.glob(os.path.join(src_root, "**", fqcn.rsplit(".", 1)[-1] + ".java"),
                         recursive=True)
        if not hits:
            return [], ""
        path = hits[0]
    try:
        src = open(path, encoding="utf-8", errors="replace").read()
    except OSError:
        return [], ""
    m = re.search(r"\bvoid\s+" + re.escape(method) + r"\s*\(", src)
    if not m:
        return [], ""
    i = src.find("{", m.end())
    if i < 0:
        return [], ""
    depth, j = 1, i + 1
    while j < len(src) and depth:
        c = src[j]
        if c == "{":
            depth += 1
        elif c == "}":
            depth -= 1
        j += 1
    body = src[i + 1:j - 1]
    steps = []
    for call_args in _iter_call_args(body, r"logStep(?:WithScreenshot)?\s*\("):
        lits = _STR_LIT.findall(call_args)
        if not lits:
            continue
        text = _clean_step(" ".join(lits).replace("\\n", " ").replace('\\"', '"'))
        if text and not text.startswith("✅") and not _FAILED_ROW.match(text):
            steps.append(text)
    desc = ""
    ct = re.search(r'createTest\s*\(([^;]*?)\)\s*;', body, re.S)
    if ct:
        lits = _STR_LIT.findall(ct.group(1))
        if lits:
            desc = lits[0]
    return steps, desc


def prettify_method(method: str) -> (str, str):
    """'TC_AF_101_atsArcFlash' → ('TC_AF_101', 'Ats arc flash')."""
    m = re.match(r"^((?:[A-Z][A-Za-z0-9]*_)+\d+[a-z]?)_(.+)$", method)
    if m:
        tcid, rest = m.group(1), m.group(2)
    else:
        tcid, rest = method, ""
    words = re.sub(r"(?<=[a-z0-9])(?=[A-Z])", " ", rest.replace("_", " ")).strip()
    return tcid, (words[:1].upper() + words[1:]) if words else ""


# ── Plain-English issue classification ────────────────────────────────────────
#
# Customers should not have to read an assertion to learn what broke. Every bug
# gets an "Issue" line naming the defect in ordinary words — "The Save button did
# not respond", "No validation message was shown" — derived from the assertion
# text, the failing step and (for unresponsive controls) the screenshots.

_CONTROL_WORDS = (r"button|link|tab|toggle|switch|checkbox|field|dropdown|picker|menu|"
                  r"chip|icon|control|option|row|card|banner|sheet|dialog|popup")

# The control the assertion talks about: "'Save Changes' button", "Save button",
# "Create button should be enabled".
_CTRL_QUOTED = re.compile(r"['‘’\"]([^'‘’\"]{2,40})['‘’\"]\s+(?:" + _CONTROL_WORDS + r")\b", re.I)
_CTRL_PLAIN = re.compile(r"\b([A-Z][\w+ ]{1,28}?)\s+(" + _CONTROL_WORDS + r")\b")

_ACTION_VERB = re.compile(
    r"\b(tap|taps|tapped|tapping|click|clicks|clicked|press|presses|pressed|"
    r"submit|submits|save|saves|saving|create|creates|select|selects|enter|"
    r"type|types|typed|choose|chooses|confirm|confirms|delete|deletes|apply)\b", re.I)
_LEADING_VERB = re.compile(
    r"^\s*(?:tap|tapping|tapped|click|clicking|clicked|press|pressing|pressed|"
    r"submit|submitting|open|opening|use|using|hit|hitting)(?:ing)?\s+(?:the\s+)?", re.I)


def extract_control(*texts):
    """Name the on-screen control an assertion is about, if it names one."""
    for t in texts:
        if not t:
            continue
        m = _CTRL_QUOTED.search(t)
        if m:
            return m.group(1).strip()
        m = _CTRL_PLAIN.search(t)
        if m:
            # "Clicking Save Changes button" → "Save Changes button". Strip only a
            # LEADING verb: 'Save' is itself an action verb, and a blanket strip
            # turned the Save Changes button into " Changes button".
            name = _LEADING_VERB.sub("", m.group(1)).strip(" -–—:,")
            if name and name.lower() not in ("the", "a", "an", "this", "that",
                                             "each", "no", "any"):
                return f"{name} {m.group(2).lower()}"
    return ""


ISSUE_TYPES = {
    "unresponsive": "Control does not respond",
    "validation": "Validation / error message missing",
    "not_saved": "Data not saved",
    "missing": "Element missing from screen",
    "not_rendered": "Content does not render",
    "navigation": "Screen does not open",
    "wrong_value": "Incorrect value displayed",
    "state": "Control in the wrong state",
    "crash": "App stops responding",
    "timeout": "Screen unresponsive (timed out)",
    "slow": "Performance below expectation",
    "other": "Verification failed",
}


def _looks_like_value(s):
    """True for things worth quoting to a customer as a value ('Saved', '480V'),
    false for requirement sentences ('Create button should be disabled when …')
    that some assertions put in the Expected slot."""
    s = (s or "").strip()
    if not s or len(s) > 45:
        return False
    return not re.search(r"\b(must|should|shall|expected to|verify|verifies)\b", s, re.I)


def classify_issue(fail_text, exc_line, method, steps, expected, actual,
                   session_dead=False, timed_out=False, no_screen_change=False,
                   last_action=""):
    """Return (issue_key, plain_english_sentence). Evidence-driven, never invented."""
    blob = " ".join(filter(None, [fail_text, exc_line, method])).lower()
    control = extract_control(fail_text, exc_line, last_action)
    # Only quote Expected/Actual back to the reader when they are actual values.
    exp_v = expected if _looks_like_value(expected) else ""
    act_v = actual if _looks_like_value(actual) else ""

    if session_dead:
        return "crash", ("The app stopped responding during this step and had to be "
                         "restarted — this is a crash signature.")
    if timed_out:
        return "timeout", ("The screen stopped responding: the flow never completed within "
                           "its time limit.")
    # A tap that changes nothing on screen is the clearest "button not working" proof.
    if no_screen_change and _ACTION_VERB.search(last_action or ""):
        if control:
            subject = f"The {control} did not respond"
        else:
            act = next((a.strip() for a in (last_action or "").split(" · ")
                        if _ACTION_VERB.search(a)), "")
            subject = (f"The step “{act[:70]}” had no effect" if act
                       else "The action in this step had no effect")
        return "unresponsive", (
            f"{subject} — the screen is pixel-for-pixel identical before and after it, "
            f"so nothing happened when the control was used.")

    if re.search(r"\b(validation|error message|warning|toast|alert)\b", blob) and \
       re.search(r"\b(missing|not shown|no |never|absent|should (?:show|appear|be shown))\b", blob):
        return "validation", ("No validation message was shown. The app accepted the input "
                              "without telling the user what was wrong.")
    # These messages are all FAILED assertions, so a requirement phrased positively
    # ("the room must be saved") already means it was not met — a negation word is
    # not required, and demanding one silently misfiled every save defect.
    if re.search(r"\b(?:be|was|is|get|got|gets) saved\b|\bsaved successfully\b|"
                 r"\bsave(?:d)? evidence\b|\bpersist(?:ed|s)?\b|\bnot saved\b|"
                 r"\bnever saved\b|\bretain(?:ed|s)?\b|\bstill shows\b", blob):
        return "not_saved", (f"The change was not saved. After saving, the screen does not "
                             f"show the new value"
                             + (f" (it still shows “{act_v}”)." if act_v else "."))
    if re.search(r"\b(must open|should open|did not open|navigate|opens? from|close back|"
                 r"return(?:s|ed)? to)\b", blob):
        return "navigation", (f"The expected screen did not open"
                              + (f" after using {control}." if control else
                                 " after this step — the app stayed on the previous screen."))
    if re.search(r"\b(enabled|disabled|selected|checked|greyed|grayed|active)\b", blob):
        return "state", (f"{('The ' + control) if control else 'The control'} is in the wrong "
                         f"state" + (f" — expected “{exp_v}”, found “{act_v or 'nothing'}”."
                                     if exp_v else "."))
    if re.search(r"\b(no |not found|never (?:visible|appear)|missing|absent|"
                 r"must be (?:visible|present|displayed)|should be (?:visible|present|displayed))\b",
                 blob):
        return "missing", (f"{('The ' + control) if control else 'An expected item'} is not "
                           f"present on the screen where the user needs it.")
    if re.search(r"\b(render|expose|display|show)\b", blob):
        return "not_rendered", ("The screen does not display the information it should — the "
                                "expected content is absent.")
    if re.search(r"\b(slow|within \d+ ?(?:s|sec|seconds|ms)|performance|took)\b", blob):
        return "slow", "The screen took longer to respond than the acceptable limit."
    if exp_v and act_v and exp_v != act_v:
        return "wrong_value", (f"The screen shows the wrong value — it displays “{act_v}” "
                               f"where “{exp_v}” is expected.")
    if exp_v and not actual:
        return "not_saved", (f"The expected value “{exp_v}” is missing — the field reads "
                             f"empty instead.")
    return "other", ""


# ── Bug field composition ─────────────────────────────────────────────────────

_EXPECTED_ACTUAL = re.compile(r"^(.*?)\s*[-—]\s*Expected:\s*(.*?),\s*Actual:\s*(.*)$", re.S)
# Selenium/Appium wait failures leak raw locators; translate to plain English.
_WAIT_FAIL = re.compile(
    r"Expected condition failed:\s*waiting for\s*(?P<what>[^:]+):.*?"
    r"(?:accessibilityId|iOSNsPredicateString|xpath|id)\s*:\s*(?P<target>[^}\)\],]+)"
    r".*?tried for (?P<secs>\d+) second", re.S | re.I)


def humanize_wait_failure(msg: str) -> str:
    """'Expected condition failed: waiting for element to be clickable: Located by
    By.chained({AppiumBy.accessibilityId: Continue}) (tried for 10 second(s)…)'
    → 'The control "Continue" never became tappable (waited 10 seconds).'"""
    m = _WAIT_FAIL.search(msg or "")
    if not m:
        return ""
    what = m.group("what").strip().lower()
    target = m.group("target").strip().strip("'\"")
    secs = m.group("secs")
    if len(target) > 60:
        target = target[:57].rstrip() + "…"
    if "clickable" in what:
        state = "never became tappable"
    elif "visib" in what or "presence" in what:
        state = "never appeared on screen"
    elif "invisib" in what or "staleness" in what:
        state = "never disappeared from the screen"
    else:
        state = "did not reach the expected state"
    return f'The control “{target}” {state} (waited {secs} seconds).'
_TESTNG_NOISE = re.compile(r"\s*expected \[(.*?)\] but found \[(.*?)\]\s*$", re.S)

HIGH_KEYS = ("crash", "sigabrt", "session dead", "app is dead", "blank screen",
             "not saved", "save failed", "data loss", "lost", "sync failed",
             "did not persist", "deleted unexpectedly", "verificationerror",
             "no such window", "terminated")
LOW_KEYS = ("placeholder", "label text", "spelling", "icon ", "alignment",
            "truncated text", "font", "color")


def _norm_level(value, default="Medium"):
    """Clamp to the three levels the PDF can render — an unknown value from an
    overrides file must never KeyError and take the whole report down."""
    v = str(value or default).strip().title()
    if v in SEV_COLOR:
        return v
    alias = {"Critical": "High", "Blocker": "High", "Major": "High",
             "Minor": "Low", "Trivial": "Low", "Normal": "Medium"}
    mapped = alias.get(v)
    if mapped:
        return mapped
    print(f"  ! override level '{value}' not one of High/Medium/Low — using {default}",
          file=sys.stderr)
    return default


def severity_for(text: str, overrides, tcid: str, cls: str):
    for o in overrides:
        pat = o.get("match", "")
        if pat and (pat in tcid or pat in cls):
            return (_norm_level(o.get("severity")), _norm_level(o.get("priority")),
                    str(o.get("note", "")))
    low = text.lower()
    if any(k in low for k in HIGH_KEYS):
        return "High", "High", ""
    if any(k in low for k in LOW_KEYS):
        return "Low", "Low", ""
    return "Medium", "Medium", ""


MODULE_PRECONDITIONS = {
    "User Login & Access": ["The app is installed and the user is logged out."],
    "Site & Facility Selection": ["User is logged in with valid QA credentials."],
}
DEFAULT_PRECONDITIONS = [
    "User is logged in with valid QA credentials.",
    "A site is selected (first available site on the account).",
]
AREA_ACCESS = {
    "Issue Tracking": "User has access to the Issues module.",
    "Asset Management": "User has access to the Assets module.",
    "Electrical Connections": "User has access to the Connections module.",
    "Locations (Buildings, Floors, Rooms)": "User has access to the Locations module.",
    "Site Visits & Work Orders": "User has access to Work Orders / Site Visits.",
    "Work Order Planning": "User has access to Work Order Planning.",
    "Work Types": "User has access to Work Orders (work-type flows).",
    "Offline & Sync": "Device network conditions can be toggled (offline mode).",
    "Arc Flash": "User has access to Arc Flash readings.",
    "Asset Engineering (Equipment Library)": "Equipment Library feature flag is enabled for the company.",
}


def compose_bug(rec, args, overrides, seq):
    method, cls = rec["method"], rec["cls"]
    tcid, pretty = prettify_method(method)
    area = area_for(rec["module"], cls)

    # 1) mine the detailed report from the best evidence root
    block = None
    used_root = None
    for root_dir in rec.get("evidence_roots", []):
        for rep in detailed_reports_for(root_dir):
            block = rep.find(method)
            if block:
                used_root = root_dir
                break
        if block:
            break

    steps, fail_texts, shots = ([], [], [])
    description = pretty
    steps_source = "template"
    if block:
        steps, fail_texts, shots = steps_from_block(block)
        if steps:
            steps_source = "report"
        name = block["name"]
        if " - " in name:
            description = name.split(" - ", 1)[1].strip() or pretty
    if len(steps) < 2:
        src_steps, src_desc = steps_from_source(args.src, cls, method)
        if len(src_steps) > len(steps):
            steps = src_steps
            steps_source = "source"
        if src_desc and " - " in src_desc and description == pretty:
            description = src_desc.split(" - ", 1)[1].strip()
        elif src_desc and description == pretty and not src_desc.startswith(tcid):
            description = src_desc

    # 2) failure screenshot from disk (full-size PNG) — best evidence, goes first
    png = None
    for root_dir in rec.get("evidence_roots", []):
        png = failure_png_for(root_dir, method)
        if png:
            break

    # 3) actual / expected
    exc_first = (rec.get("exc_msg") or "").strip().splitlines()
    exc_line = exc_first[0].strip() if exc_first else ""
    primary_fail = fail_texts[0] if fail_texts else exc_line
    expected, actual, label = "", "", ""
    timeout = bool(_TIMEOUT_MSG.search(primary_fail or "") or _TIMEOUT_MSG.search(exc_line))
    exc_only = bool(_EXC_ONLY.match((primary_fail or "").strip()))
    scenario = description or pretty or method
    _dead_sigs = ("session dead", "session is either terminated",
                  "session likely dead", "app is dead")
    _dead_basis = ((exc_line or "") + " " + (primary_fail or "")).lower()
    session_dead = any(sig in _dead_basis for sig in _dead_sigs)

    humanized_wait = humanize_wait_failure(primary_fail) or humanize_wait_failure(exc_line)

    if session_dead:
        label = ""
        actual_result = ("The app stopped responding during this flow and the "
                         "automation session was lost — a typical crash signature. "
                         "No screenshot could be captured at the failure moment.")
        expected_result = (f"The flow should complete normally: {scenario}."
                           if scenario else "The flow should complete normally.")
    elif humanized_wait:
        label = ""
        actual_result = humanized_wait + " The screen state at that moment is shown " \
                                         "in the screenshot."
        expected_result = (f"The flow should complete normally: {scenario}."
                           if scenario else "The control should be available.")
    elif timeout:
        label = ""
        actual_result = ("The flow did not complete: the screen stopped responding or a "
                         "required element never appeared, and the test timed out after "
                         "its 8-minute budget. The app state at the timeout is shown in "
                         "the screenshot.")
        expected_result = (f"The flow should complete normally: {scenario}."
                           if scenario else "The flow should complete normally.")
    elif exc_only:
        label = ""
        actual_result = (f"The step never finished — an internal wait timed out "
                         f"({primary_fail.strip()}) while executing: {scenario}.")
        expected_result = (f"The flow should complete normally: {scenario}."
                           if scenario else "The flow should complete normally.")
    else:
        m = _EXPECTED_ACTUAL.match(primary_fail or "")
        if m:
            label = m.group(1).strip().rstrip(".")
            expected = m.group(2).strip()
            actual = m.group(3).strip()
        else:
            n = _TESTNG_NOISE.search(primary_fail or "")
            if n:
                label = _TESTNG_NOISE.sub("", primary_fail).strip().rstrip(".:")
                expected, actual = n.group(1), n.group(2)
            else:
                label = (primary_fail or "").strip().rstrip(".")
        label = _ASSERT_PREFIX.sub("", label).strip()
        label = _IDENT_PREFIX.sub("", label).strip()

        if expected or actual:
            actual_result = (f"{label}. " if label else "") + \
                f"Observed value: “{actual or '(empty)'}” — expected “{expected or '(see below)'}”."
            if expected:
                expected_result = (f"{label}. " if label else "") + \
                    f"The value should be “{expected}”."
            else:
                expected_result = scenario or "The verification should pass."
        else:
            # A label phrased as a requirement ("X must/should …") is the broken
            # expectation, not the observation — say so explicitly.
            if label and re.search(r"\b(must|should)\b", label):
                actual_result = f"The requirement was not met: “{label}”. " \
                                "See the screenshot for the app state at failure."
                if not expected:
                    expected = label
            else:
                actual_result = label or \
                    "The test's verification failed at the step shown in the screenshot."
            # Append the raw exception line only when it adds information the
            # label doesn't already carry (avoid the duplicated-paragraph look).
            if exc_line:
                a, b = exc_line[:60].lower(), (label or "")[:60].lower()
                if a not in (label or "").lower() and b not in exc_line.lower():
                    actual_result += f" ({exc_line})"
            expected_result = expected or scenario or \
                "The screen should behave as designed for this flow."
            if expected_result.lower().startswith("verify"):
                expected_result = re.sub(r"^verify(?:ing|s)?[:\s]*", "", expected_result,
                                         flags=re.I)
                expected_result = expected_result[:1].upper() + expected_result[1:] + \
                    " — this verification should succeed."
            elif not expected_result.lower().startswith(("the ", "should")):
                expected_result = "Expected behavior: " + expected_result

    actual_result = actual_result[:1].upper() + actual_result[1:]
    expected_result = expected_result[:1].upper() + expected_result[1:]

    # 3b) Visual evidence: compare the screen at the last action with the screen at
    # failure. Identical pixels after a tap is direct proof the control did nothing.
    action_shot = next((s for s in shots if s["role"] == "action"), None)
    failure_shot = next((s for s in shots if s["role"] == "failure"), None)
    diff_boxes, no_change = [], False
    if action_shot and failure_shot:
        a_img, f_img = _load_pil(action_shot["uri"]), _load_pil(failure_shot["uri"])
        if a_img is not None and f_img is not None:
            diff_boxes, frac = diff_regions(a_img, f_img)
            no_change = frac < 0.0004
    # Name the control from the most recent step that actually performs an action
    # ("Clicking Save Changes button directly"), not merely the last log line
    # ("Save completed: false"), which names nothing the user can tap.
    action_texts = [s for s in steps if _ACTION_VERB.search(s)]
    last_action = " · ".join(
        [t for t in [(action_shot or {}).get("caption", ""),
                     action_texts[-1] if action_texts else "",
                     steps[-1] if steps else ""] if t])

    issue_key, issue_text = classify_issue(
        primary_fail, exc_line, method, steps, expected, actual,
        session_dead=session_dead, timed_out=timeout or exc_only,
        no_screen_change=no_change, last_action=last_action)
    # The issue statement gets its own highlighted line on the page, so the Actual
    # Result keeps the specific evidence rather than repeating that sentence.

    sev_basis = " ".join([primary_fail, exc_line, rec.get("exc_class", ""),
                          rec.get("exc_stack", "")])
    severity, priority, note = severity_for(sev_basis, overrides, tcid, cls)
    if issue_key in ("crash", "not_saved") and severity == "Medium":
        severity = priority = "High"

    # 4) title — a defect statement, not the test name
    if session_dead:
        title_core = f"{scenario} — app stopped responding (possible crash)"
    elif humanized_wait:
        title_core = humanized_wait.rstrip(".").replace("The control ", "Control ")
    elif timeout or exc_only:
        title_core = f"{scenario} — screen unresponsive (flow timed out)"
    elif label and len(label) <= 90:
        title_core = label
    elif scenario:
        title_core = f"{scenario} — verification failed"
    else:
        title_core = label or f"{pretty or method} fails"
    title_core = re.sub(r"^(?:verify|verifying|verifies)\s*[:\-]?\s*", "", title_core,
                        flags=re.I)
    title_core = title_core[:1].upper() + title_core[1:]
    if len(title_core) > 110:
        title_core = title_core[:107].rstrip() + "…"
    case = (rec.get("params") or "").strip()
    if case:
        title_core += f" [case: {case}]"

    # 5) preconditions & steps
    pre = list(MODULE_PRECONDITIONS.get(area, DEFAULT_PRECONDITIONS))
    if area in AREA_ACCESS:
        pre.append(AREA_ACCESS[area])
    launch = "Launch the Z Platform iOS app."
    login = None
    if area not in MODULE_PRECONDITIONS:
        login = "Log in with valid QA credentials and select the test site."
    # If the mined steps already start with a login/navigation step, don't
    # duplicate the implicit one.
    if login and steps and re.match(r"^\s*(log\s*in|login|sign\s*in)", steps[0], re.I):
        login = None
    full_steps = [launch] + ([login] if login else [])
    if case:
        full_steps.append(f"Use this test case / input: {case}.")
    if steps:
        full_steps += steps
    else:
        full_steps += [f"Navigate to the {area} area of the app.",
                       f"Execute the scenario: {description or pretty or method}."]
    truncated = 0
    if len(full_steps) > args.max_steps:
        truncated = len(full_steps) - args.max_steps
        full_steps = full_steps[:args.max_steps]

    reproducibility = (
        "Failed in the main run and reproduced on an isolated re-run "
        "(fresh simulator, fresh login) — deterministic."
        if rec.get("reran") else
        (getattr(args, "repro_note", "") or
         "Failed in the main run; the verification re-run did not reach this test.")
    )

    return {
        "seq": seq,
        "bug_id": f"BUG-{args.run_id[-6:] if args.run_id else 'LOCAL'}-{seq:03d}",
        "title": f"[{area}] {title_core}",
        "area": area,
        "module": rec["module"],
        "tcid": tcid + (f" [{case}]" if case else ""),
        "test_ref": f"{cls.rsplit('.', 1)[-1]}#{method}",
        "description": description or pretty or method,
        "severity": severity,
        "priority": priority,
        "note": note,
        "preconditions": pre,
        "steps": full_steps,
        "steps_truncated": truncated,
        "steps_source": steps_source,
        "actual": actual_result,
        "expected": expected_result,
        "exc_class": rec.get("exc_class", ""),
        "exc_stack": rec.get("exc_stack", ""),
        "duration_ms": rec.get("ms", 0),
        "reproducibility": reproducibility,
        "png": png,
        "shots": shots,
        "issue_key": issue_key,
        "issue_type": ISSUE_TYPES.get(issue_key, ISSUE_TYPES["other"]),
        "issue_text": issue_text,
        "no_screen_change": no_change,
        "diff_boxes": diff_boxes,
        "session_dead": session_dead,
        "evidence_root": used_root,
    }


# ── Jira CSV export ───────────────────────────────────────────────────────────
#
# One "Bug" row per defect, importable via Jira's CSV importer (External System
# Import → CSV). Descriptions use Jira wiki markup (h3. / * / #) so the sections
# render as headings and lists after import. We only produce the FILE — tickets
# are never created against a live Jira (standing rule: no external systems).

def _jira_label(s):
    return re.sub(r"-{2,}", "-", re.sub(r"[^a-z0-9]+", "-", (s or "").lower())).strip("-")


def jira_description(b, args):
    lines = []
    if b.get("issue_text"):
        lines += ["h3. Issue", b["issue_text"], ""]
    lines += ["h3. Environment",
              f"* Environment: {args.env_name}",
              f"* Platform: {args.platform}",
              f"* App Version: {args.app_version or 'not recorded for this run'}",
              f"* Device: {args.device} (iOS {args.ios_version})",
              ""]
    lines += ["h3. Preconditions"]
    lines += [f"* {p}" for p in b["preconditions"]]
    lines += ["", "h3. Steps to Reproduce"]
    lines += [f"# {s}" for s in b["steps"]]
    if b.get("steps_truncated"):
        lines += [f"# …plus {b['steps_truncated']} further scripted steps "
                  f"(see automated test {b['test_ref']})"]
    lines += ["", "h3. Actual Result", b["actual"],
              "", "h3. Expected Result", b["expected"],
              "", "h3. Reproducibility", b["reproducibility"]]
    if b.get("note"):
        lines += ["", "h3. Note", b["note"]]
    shots = []
    if b.get("png"):
        shots.append(os.path.basename(b["png"]))
    n_inline = len(b.get("shots", []))
    lines += ["", "h3. Attachments",
              f"* Annotated screenshots ({n_inline} panels: before / action / at failure) "
              f"are embedded under entry {b['bug_id']} in the Defect Report PDF for this run."]
    if shots:
        lines += [f"* Failure capture file: {shots[0]} (in the run's {b.get('module', '')} "
                  f"artifact, screenshots/ folder)"]
    if args.run_url:
        lines += [f"* CI run: {args.run_url}"]
    return "\n".join(lines)


def emit_jira_csv(bugs, args, path):
    import csv
    os.makedirs(os.path.dirname(os.path.abspath(path)), exist_ok=True)
    # utf-8-sig so Excel/Jira both read the BOM and the em-dashes survive.
    with open(path, "w", newline="", encoding="utf-8-sig") as fh:
        w = csv.writer(fh, quoting=csv.QUOTE_ALL)
        w.writerow(["Summary", "Issue Type", "Severity", "Priority", "Component",
                    "Affects Version", "Labels", "Bug ID", "Test Reference",
                    "Issue Category", "Reproducibility", "Description"])
        for b in bugs:
            labels = " ".join(filter(None, [
                "qa-automation", "ios",
                _jira_label(b["area"]),
                _jira_label(b.get("issue_key", "")),
                f"run-{args.run_id}" if args.run_id else "",
                "rerun-confirmed" if "deterministic" in b["reproducibility"] else "not-rerun",
            ]))
            w.writerow([
                b["title"],
                "Bug",
                b["severity"],
                b["priority"],
                b["area"],
                args.app_version or "",
                labels,
                b["bug_id"],
                b["test_ref"],
                b.get("issue_type", ""),
                b["reproducibility"],
                jira_description(b, args),
            ])
    size_kb = os.path.getsize(path) / 1024.0
    print(f"  wrote {path} ({size_kb:.0f} KB, {len(bugs)} Jira bug rows)")


# ── App / environment metadata ────────────────────────────────────────────────

def detect_app_version(plist_path: str) -> str:
    """Best-effort app version — never a WRONG one.

    Order: (1) the git commit subject that last touched apps/ ("chore(app): update
    Z Platform-QA to v1.55 for CI") — authoritative, but unavailable on the shallow
    sparse checkout CI uses, hence --app-version in the workflows; (2) the bundle's
    Info.plist, which is KNOWN to lag (the v1.55 bundle still declares 1.49) so it
    is labelled as such; (3) empty → the cover prints "not recorded" rather than a
    version that may be false.
    NB: only apps/Z-Platform-QA.zip is tracked (the .app is unzipped at run time),
    so the pathspec must be the apps/ directory, not the .app bundle.
    """
    try:
        shallow = subprocess.run(["git", "rev-parse", "--is-shallow-repository"],
                                 capture_output=True, text=True, timeout=15
                                 ).stdout.strip() == "true"
        if not shallow:
            out = subprocess.run(
                ["git", "log", "-1", "--format=%s", "--", "apps"],
                capture_output=True, text=True, timeout=15).stdout.strip()
            m = re.search(r"Z[\s-]?Platform[\w-]*\s+to\s+v(\d+(?:\.\d+)+)", out) \
                or re.search(r"\bapp\b.*\bv(\d+(?:\.\d+)+)", out, re.I)
            if m:
                return f"Z Platform-QA v{m.group(1)}"
    except Exception:
        pass
    try:
        import plistlib
        with open(plist_path, "rb") as fh:
            p = plistlib.load(fh)
        v = p.get("CFBundleShortVersionString", "")
        name = p.get("CFBundleName", "Z Platform-QA")
        return f"{name} v{v} (per app bundle)" if v else ""
    except Exception:
        return ""


# ── PDF rendering ─────────────────────────────────────────────────────────────

NAVY = "#0f2a4a"
BLUE = "#1a56db"
RED = "#b42318"
GREEN = "#067647"
GREY = "#667085"
LIGHT = "#f4f6fa"
BORDER = "#d6dce5"

SEV_COLOR = {"High": "#b42318", "Medium": "#b54708", "Low": "#175cd3"}


def _load_pil(source):
    """PNG path or data-URI → RGB PIL image, or None."""
    from PIL import Image as PILImage
    try:
        if isinstance(source, str) and source.startswith("data:image/"):
            b64 = source.split(",", 1)[1]
            raw = base64.b64decode(b64 + "=" * (-len(b64) % 4))
            pil = PILImage.open(io.BytesIO(raw))
        else:
            pil = PILImage.open(source)
        return pil.convert("RGB")
    except Exception as e:
        print(f"  ! image skipped ({e})", file=sys.stderr)
        return None


def _fingerprint(pil):
    """Tiny perceptual hash so the report never shows the same screen twice."""
    from PIL import Image as PILImage
    small = pil.convert("L").resize((16, 16), PILImage.BILINEAR)
    px = list(small.getdata())
    avg = sum(px) / len(px)
    return "".join("1" if p > avg else "0" for p in px)


def _hamming(a, b):
    return sum(1 for x, y in zip(a, b) if x != y) if a and b and len(a) == len(b) else 999


def diff_regions(before, after, min_frac=0.0004):
    """Bounding boxes of what changed between two screenshots.

    Returns (boxes, changed_fraction). Compression noise is filtered by an
    absolute-difference threshold; boxes are merged row-bands so a highlight
    frames a UI region rather than scattering over glyph edges.
    """
    from PIL import Image as PILImage, ImageChops
    if before is None or after is None:
        return [], 0.0
    if before.size != after.size:
        after = after.resize(before.size, PILImage.BILINEAR)
    diff = ImageChops.difference(before.convert("L"), after.convert("L"))
    mask = diff.point(lambda p: 255 if p > 34 else 0)
    w, h = mask.size
    px = mask.load()
    step = max(1, h // 400)
    rows, changed = [], 0
    for y in range(0, h, step):
        xs = [x for x in range(0, w, max(1, w // 200)) if px[x, y]]
        if xs:
            changed += len(xs)
            rows.append((y, min(xs), max(xs)))
    total = (h // step) * (w // max(1, w // 200))
    frac = changed / total if total else 0.0
    if frac < min_frac or not rows:
        return [], frac
    # Merge adjacent changed rows into bands (gap tolerance = 3% of height).
    gap = max(4, int(h * 0.03))
    bands, cur = [], [rows[0][0], rows[0][0], rows[0][1], rows[0][2]]
    for y, x0, x1 in rows[1:]:
        if y - cur[1] <= gap:
            cur[1] = y
            cur[2] = min(cur[2], x0)
            cur[3] = max(cur[3], x1)
        else:
            bands.append(cur)
            cur = [y, y, x0, x1]
    bands.append(cur)
    pad = max(6, int(h * 0.008))
    boxes = [(max(0, x0 - pad), max(0, y0 - pad), min(w, x1 + pad), min(h, y1 + step + pad))
             for y0, y1, x0, x1 in bands
             if (y1 - y0) > h * 0.004 or (x1 - x0) > w * 0.05]
    boxes.sort(key=lambda b: (b[3] - b[1]) * (b[2] - b[0]), reverse=True)
    return boxes[:4], frac


def annotate(pil, banner_text="", boxes=(), accent=(200, 35, 24)):
    """Draw the highlight boxes and an issue banner onto a screenshot."""
    from PIL import ImageDraw, ImageFont
    img = pil.copy()
    draw = ImageDraw.Draw(img, "RGBA")
    w, h = img.size
    stroke = max(3, int(w * 0.008))
    for (x0, y0, x1, y1) in boxes:
        draw.rectangle([x0, y0, x1, y1], outline=accent + (255,), width=stroke)
        draw.rectangle([x0, y0, x1, y1], fill=accent + (26,))
    if banner_text:
        pad = max(10, int(w * 0.022))
        # The banner has to stay readable after the page shrinks the shot to a
        # half-column thumbnail, so it is sized generously against image width.
        size = max(22, int(w * 0.055))
        font = None
        for cand in ("/System/Library/Fonts/Supplemental/Arial Bold.ttf",
                     "/System/Library/Fonts/Helvetica.ttc",
                     "/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf",
                     "/usr/share/fonts/truetype/liberation/LiberationSans-Bold.ttf"):
            try:
                font = ImageFont.truetype(cand, size)
                break
            except Exception:
                continue
        if font is None:
            font = ImageFont.load_default()
        # Wrap to the image width.
        words, lines, cur = banner_text.split(), [], ""
        for word in words:
            trial = (cur + " " + word).strip()
            if draw.textlength(trial, font=font) > w - 2 * pad and cur:
                lines.append(cur)
                cur = word
            else:
                cur = trial
        if cur:
            lines.append(cur)
        lines = lines[:3]
        lh = int(size * 1.3)
        bh = lh * len(lines) + pad
        draw.rectangle([0, 0, w, bh], fill=accent + (243,))
        for i, ln in enumerate(lines):
            draw.text((pad, pad // 2 + i * lh), ln, fill=(255, 255, 255, 255), font=font)
    return img


def _pil_to_flowable(pil, max_w, max_h, jpeg_quality, shot_px=700):
    from reportlab.platypus import Image as RLImage
    buf = io.BytesIO()
    shrunk = pil.copy()
    # Panels render ~250pt wide, so anything past ~700px is bytes the reader
    # never sees — and with 4 shots per defect it is what pushes the report
    # past the email attachment limit.
    shrunk.thumbnail((shot_px, shot_px))
    shrunk.save(buf, format="JPEG", quality=jpeg_quality, optimize=True)
    buf.seek(0)
    w, h = shrunk.size
    scale = min(max_w / w, max_h / h, 1.0)
    return RLImage(buf, width=w * scale, height=h * scale)


def _img_flowable(source, max_w, max_h, jpeg_quality):
    pil = _load_pil(source)
    if pil is None:
        return None
    return _pil_to_flowable(pil, max_w, max_h, jpeg_quality)


def _build_panels(b, args):
    """The screenshot narrative for one bug: [(PIL image, caption), …].

    Order: context → the action taken → the moment of failure (annotated with the
    issue banner and highlight boxes) → the full-size failure capture. Visually
    identical shots are dropped so the reader never sees the same screen twice.
    """
    panels, seen = [], []

    def add(pil, caption):
        if pil is None or len(panels) >= args.max_shots:
            return
        fp = _fingerprint(pil)
        if any(_hamming(fp, s) <= 3 for s in seen):
            return
        seen.append(fp)
        panels.append((pil, caption))

    role_label = {"context": "Before", "action": "Action taken", "failure": "At failure"}
    ordered = sorted(b.get("shots", []),
                     key=lambda s: {"context": 0, "action": 1, "failure": 2}[s["role"]])
    failure_uri = next((s["uri"] for s in ordered if s["role"] == "failure"), None)

    for s in ordered:
        pil = _load_pil(s["uri"])
        if pil is None:
            continue
        caption = f"{role_label[s['role']]} — {s['caption']}" if s["caption"] \
            else role_label[s["role"]]
        if s["role"] == "failure":
            banner = b.get("issue_text") or b.get("actual", "")
            pil = annotate(pil, banner[:150], b.get("diff_boxes") or ())
            if b.get("no_screen_change"):
                caption += " · identical to the previous screen"
            elif b.get("diff_boxes"):
                caption += " · red outline marks what changed"
        add(pil, caption)

    # The on-disk PNG is the full-resolution capture; include it when it is not
    # simply a duplicate of the inline failure shot.
    if b.get("png"):
        disk = _load_pil(b["png"])
        if disk is not None:
            if failure_uri is None:
                banner = b.get("issue_text") or b.get("actual", "")
                disk = annotate(disk, banner[:150], b.get("diff_boxes") or ())
            add(disk, "Full-resolution capture taken when the test failed")
    return panels


def build_pdf(bugs, stats, args):
    from reportlab.lib import colors
    from reportlab.lib.pagesizes import A4
    from reportlab.lib.styles import ParagraphStyle
    from reportlab.lib.units import inch
    from reportlab.platypus import (BaseDocTemplate, Frame, PageTemplate, PageBreak,
                                    Paragraph, Spacer, Table, TableStyle, KeepTogether)

    page_w, page_h = A4
    margin = 0.55 * inch
    content_w = page_w - 2 * margin

    styles = {
        "cover_title": ParagraphStyle("ct", fontName="Helvetica-Bold", fontSize=24,
                                      textColor=colors.white, leading=30),
        "cover_sub": ParagraphStyle("cs", fontName="Helvetica", fontSize=12,
                                    textColor=colors.HexColor("#cfe0f5"), leading=16),
        "h1": ParagraphStyle("h1", fontName="Helvetica-Bold", fontSize=15,
                             textColor=colors.HexColor(NAVY), spaceAfter=8, leading=19),
        "bug_id": ParagraphStyle("bid", fontName="Helvetica-Bold", fontSize=9,
                                 textColor=colors.HexColor(BLUE)),
        "bug_title": ParagraphStyle("bt", fontName="Helvetica-Bold", fontSize=13,
                                    textColor=colors.HexColor(NAVY), leading=17,
                                    spaceAfter=2),
        "label": ParagraphStyle("lb", fontName="Helvetica-Bold", fontSize=9.5,
                                textColor=colors.HexColor(NAVY), spaceBefore=8,
                                spaceAfter=3),
        "body": ParagraphStyle("bd", fontName="Helvetica", fontSize=9.5,
                               textColor=colors.HexColor("#1d2939"), leading=13.5),
        "step": ParagraphStyle("st", fontName="Helvetica", fontSize=9.5,
                               textColor=colors.HexColor("#1d2939"), leading=13.5,
                               leftIndent=16, firstLineIndent=-16, spaceAfter=2),
        "meta": ParagraphStyle("mt", fontName="Helvetica", fontSize=8.5,
                               textColor=colors.HexColor(GREY), leading=12),
        "actual": ParagraphStyle("ac", fontName="Helvetica", fontSize=9.5,
                                 textColor=colors.HexColor(RED), leading=13.5),
        "expected": ParagraphStyle("ex", fontName="Helvetica", fontSize=9.5,
                                   textColor=colors.HexColor(GREEN), leading=13.5),
        "mono": ParagraphStyle("mo", fontName="Courier", fontSize=7.5,
                               textColor=colors.HexColor(GREY), leading=9.5),
        "caption": ParagraphStyle("cp", fontName="Helvetica-Oblique", fontSize=8,
                                  textColor=colors.HexColor(GREY), leading=11,
                                  spaceBefore=2),
        "issue": ParagraphStyle("is", fontName="Helvetica", fontSize=10.5,
                                textColor=colors.HexColor("#7a271a"), leading=14.5),
        "idx": ParagraphStyle("ix", fontName="Helvetica", fontSize=8.5,
                              textColor=colors.HexColor("#1d2939"), leading=11.5),
    }

    def esc(s):
        return html_mod.escape(str(s or ""))

    def on_page(canvas, doc):
        canvas.saveState()
        canvas.setFillColor(colors.HexColor(NAVY))
        canvas.rect(0, page_h - 0.32 * inch, page_w, 0.32 * inch, stroke=0, fill=1)
        canvas.setFillColor(colors.white)
        canvas.setFont("Helvetica-Bold", 8)
        canvas.drawString(margin, page_h - 0.22 * inch,
                          "eGalvanic — iOS Automation Defect Report")
        canvas.drawRightString(page_w - margin, page_h - 0.22 * inch,
                               args.run_date or "")
        canvas.setFillColor(colors.HexColor(GREY))
        canvas.setFont("Helvetica", 7.5)
        canvas.drawString(margin, 0.3 * inch,
                          "Confidential — prepared by the QA automation team")
        canvas.drawRightString(page_w - margin, 0.3 * inch, f"Page {doc.page}")
        canvas.restoreState()

    doc = BaseDocTemplate(args.out, pagesize=A4,
                          leftMargin=margin, rightMargin=margin,
                          topMargin=0.55 * inch, bottomMargin=0.55 * inch,
                          title="iOS Automation Defect Report",
                          author="eGalvanic QA Automation")
    frame = Frame(margin, 0.5 * inch, content_w, page_h - 1.15 * inch, id="main")
    doc.addPageTemplates([PageTemplate(id="all", frames=[frame], onPage=on_page)])

    story = []

    # ── Cover ──
    cover_rows = [
        ["Report", "iOS Automation Defect Report — Failed Test Cases with Reproduction Steps"],
        ["Run date", args.run_date or "—"],
        ["Environment", args.env_name],
        ["Platform", args.platform],
        ["Device", args.device],
        ["OS Version", args.ios_version],
        ["App Version", args.app_version or "not recorded for this run"],
        ["CI Run", args.run_url or "—"],
        ["Total defects in this report", str(len(bugs)) + (
            f"  (from {stats['failed']} failed test cases — data-driven tests "
            f"contribute one defect per failing case)"
            if len(bugs) != stats["failed"] else "")],
    ]
    cover_tbl = Table(
        [[Paragraph(f"<b>{esc(k)}</b>", styles["body"]),
          Paragraph(esc(v), styles["body"])] for k, v in cover_rows],
        colWidths=[1.7 * inch, content_w - 1.7 * inch])
    cover_tbl.setStyle(TableStyle([
        ("GRID", (0, 0), (-1, -1), 0.5, colors.HexColor(BORDER)),
        ("BACKGROUND", (0, 0), (0, -1), colors.HexColor(LIGHT)),
        ("VALIGN", (0, 0), (-1, -1), "TOP"),
        ("TOPPADDING", (0, 0), (-1, -1), 5),
        ("BOTTOMPADDING", (0, 0), (-1, -1), 5),
    ]))

    banner = Table([[Paragraph("iOS Automation<br/>Defect Report", styles["cover_title"])],
                    [Paragraph("Every failed test case from this CI run, documented with "
                               "steps to reproduce, actual vs expected results and screenshots.",
                               styles["cover_sub"])]],
                   colWidths=[content_w])
    banner.setStyle(TableStyle([
        ("BACKGROUND", (0, 0), (-1, -1), colors.HexColor(NAVY)),
        ("LEFTPADDING", (0, 0), (-1, -1), 18),
        ("RIGHTPADDING", (0, 0), (-1, -1), 18),
        ("TOPPADDING", (0, 0), (0, 0), 18),
        ("BOTTOMPADDING", (0, -1), (-1, -1), 18),
    ]))
    story += [banner, Spacer(1, 14), cover_tbl, Spacer(1, 14)]

    # run stats
    stat_rows = [["Total tests", "Passed", "Failed", "Skipped", "Pass rate"],
                 [str(stats["total"]), str(stats["passed"]), str(stats["failed"]),
                  str(stats["skipped"]), f"{stats['rate']:.1f}%"]]
    stat_tbl = Table(stat_rows, colWidths=[content_w / 5] * 5)
    stat_tbl.setStyle(TableStyle([
        ("BACKGROUND", (0, 0), (-1, 0), colors.HexColor(NAVY)),
        ("TEXTCOLOR", (0, 0), (-1, 0), colors.white),
        ("FONTNAME", (0, 0), (-1, 0), "Helvetica-Bold"),
        ("FONTNAME", (0, 1), (-1, 1), "Helvetica-Bold"),
        ("FONTSIZE", (0, 0), (-1, -1), 10),
        ("TEXTCOLOR", (2, 1), (2, 1), colors.HexColor(RED)),
        ("ALIGN", (0, 0), (-1, -1), "CENTER"),
        ("GRID", (0, 0), (-1, -1), 0.5, colors.HexColor(BORDER)),
        ("TOPPADDING", (0, 0), (-1, -1), 6),
        ("BOTTOMPADDING", (0, 0), (-1, -1), 6),
    ]))
    story += [Paragraph("Run summary (after verification re-run)", styles["h1"]), stat_tbl,
              Spacer(1, 12)]

    # per-area counts
    by_area = defaultdict(int)
    for b in bugs:
        by_area[b["area"]] += 1
    area_rows = [["Feature area", "Defects"]] + \
        [[a, str(n)] for a, n in sorted(by_area.items(), key=lambda kv: -kv[1])]
    area_tbl = Table(area_rows, colWidths=[content_w - 1.2 * inch, 1.2 * inch])
    area_tbl.setStyle(TableStyle([
        ("BACKGROUND", (0, 0), (-1, 0), colors.HexColor(LIGHT)),
        ("FONTNAME", (0, 0), (-1, 0), "Helvetica-Bold"),
        ("FONTSIZE", (0, 0), (-1, -1), 9),
        ("ALIGN", (1, 0), (1, -1), "CENTER"),
        ("GRID", (0, 0), (-1, -1), 0.5, colors.HexColor(BORDER)),
        ("TOPPADDING", (0, 0), (-1, -1), 4),
        ("BOTTOMPADDING", (0, 0), (-1, -1), 4),
    ]))
    story += [Paragraph("Defects by feature area", styles["h1"]), area_tbl, PageBreak()]

    # ── Index ──
    story.append(Paragraph("Defect index", styles["h1"]))
    idx_rows = [["ID", "Title", "Severity"]]
    for b in bugs:
        idx_rows.append([Paragraph(esc(b["bug_id"]), styles["idx"]),
                         Paragraph(esc(b["title"]), styles["idx"]),
                         Paragraph(f'<font color="{SEV_COLOR.get(b["severity"], GREY)}">'
                                   f'<b>{esc(b["severity"])}</b></font>', styles["idx"])])
    idx_tbl = Table(idx_rows, colWidths=[1.15 * inch, content_w - 1.95 * inch, 0.8 * inch],
                    repeatRows=1)
    idx_tbl.setStyle(TableStyle([
        ("BACKGROUND", (0, 0), (-1, 0), colors.HexColor(NAVY)),
        ("TEXTCOLOR", (0, 0), (-1, 0), colors.white),
        ("FONTNAME", (0, 0), (-1, 0), "Helvetica-Bold"),
        ("FONTSIZE", (0, 0), (-1, 0), 9),
        ("GRID", (0, 0), (-1, -1), 0.4, colors.HexColor(BORDER)),
        ("VALIGN", (0, 0), (-1, -1), "TOP"),
        ("ROWBACKGROUNDS", (0, 1), (-1, -1),
         [colors.white, colors.HexColor("#fafbfd")]),
        ("TOPPADDING", (0, 0), (-1, -1), 3.5),
        ("BOTTOMPADDING", (0, 0), (-1, -1), 3.5),
    ]))
    story += [idx_tbl, PageBreak()]

    # ── Bug pages ──
    img_w = content_w / 2 - 8
    img_h = 3.5 * inch
    for i, b in enumerate(bugs):
        head = [
            Paragraph(esc(b["bug_id"]) + "  ·  " + esc(b["tcid"]), styles["bug_id"]),
            Paragraph(esc(b["title"]), styles["bug_title"]),
        ]
        sev_html = (f'<font color="{SEV_COLOR.get(b["severity"], GREY)}">'
                    f'<b>{esc(b["severity"])}</b></font>')
        pri_html = (f'<font color="{SEV_COLOR.get(b["priority"], GREY)}">'
                    f'<b>{esc(b["priority"])}</b></font>')
        meta_rows = [
            ["Issue type", b.get("issue_type", ""), "Severity",
             Paragraph(sev_html, styles["body"])],
            ["Feature area", b["area"], "Priority", Paragraph(pri_html, styles["body"])],
            ["Automated test", b["test_ref"], "App version", args.app_version or "—"],
            ["Environment", args.env_name + " · " + args.platform, "Status", "Open (new)"],
            ["Device", args.device + " · iOS " + args.ios_version, "Reported", args.run_date],
        ]
        meta_tbl = Table(
            [[Paragraph(f"<b>{esc(r[0])}</b>", styles["meta"]),
              r[1] if not isinstance(r[1], str) else Paragraph(esc(r[1]), styles["meta"]),
              Paragraph(f"<b>{esc(r[2])}</b>", styles["meta"]),
              r[3] if not isinstance(r[3], str) else Paragraph(esc(r[3]), styles["meta"])]
             for r in meta_rows],
            colWidths=[1.05 * inch, content_w / 2 - 1.05 * inch - 4,
                       1.05 * inch, content_w / 2 - 1.05 * inch - 4])
        meta_tbl.setStyle(TableStyle([
            ("GRID", (0, 0), (-1, -1), 0.4, colors.HexColor(BORDER)),
            ("BACKGROUND", (0, 0), (0, -1), colors.HexColor(LIGHT)),
            ("BACKGROUND", (2, 0), (2, -1), colors.HexColor(LIGHT)),
            ("VALIGN", (0, 0), (-1, -1), "TOP"),
            ("TOPPADDING", (0, 0), (-1, -1), 4),
            ("BOTTOMPADDING", (0, 0), (-1, -1), 4),
        ]))

        section = head + [Spacer(1, 6), meta_tbl, Spacer(1, 4)]

        if b.get("issue_text"):
            issue_tbl = Table([[Paragraph("<b>What is wrong:</b> " + esc(b["issue_text"]),
                                          styles["issue"])]], colWidths=[content_w])
            issue_tbl.setStyle(TableStyle([
                ("BACKGROUND", (0, 0), (-1, -1), colors.HexColor("#fdf3f2")),
                ("LINEBEFORE", (0, 0), (0, -1), 3, colors.HexColor(RED)),
                ("LEFTPADDING", (0, 0), (-1, -1), 10),
                ("RIGHTPADDING", (0, 0), (-1, -1), 10),
                ("TOPPADDING", (0, 0), (-1, -1), 7),
                ("BOTTOMPADDING", (0, 0), (-1, -1), 7),
            ]))
            section += [Spacer(1, 4), issue_tbl]

        section.append(Paragraph("Preconditions", styles["label"]))
        for p in b["preconditions"]:
            section.append(Paragraph("•  " + esc(p), styles["step"]))

        section.append(Paragraph("Steps to Reproduce", styles["label"]))
        for n, s in enumerate(b["steps"], 1):
            section.append(Paragraph(f"{n}.  " + esc(s), styles["step"]))
        if b["steps_truncated"]:
            section.append(Paragraph(
                f"…plus {b['steps_truncated']} further scripted steps "
                f"(see automated test {esc(b['test_ref'])}).", styles["caption"]))

        section.append(Paragraph("Actual Result", styles["label"]))
        section.append(Paragraph(esc(b["actual"]), styles["actual"]))
        if b["session_dead"]:
            section.append(Paragraph(
                "The app/automation session terminated at this point — possible crash.",
                styles["actual"]))

        section.append(Paragraph("Expected Result", styles["label"]))
        section.append(Paragraph(esc(b["expected"]), styles["expected"]))

        section.append(Paragraph("Reproducibility", styles["label"]))
        section.append(Paragraph(esc(b["reproducibility"]), styles["body"]))
        if b["note"]:
            section.append(Paragraph("Note: " + esc(b["note"]), styles["body"]))
        if b["exc_stack"].strip() and args.include_stack:
            section.append(KeepTogether([
                Paragraph("Technical reference (for the engineering team)", styles["label"]),
                Paragraph(esc(b["exc_stack"]).replace("\n", "<br/>"), styles["mono"]),
            ]))

        story.append(KeepTogether(section[:3]))
        story += section[3:]

        # ── Attachments: a captioned visual narrative, failure shot annotated ──
        panels = _build_panels(b, args)
        if panels:
            cells = []
            for pil, caption in panels:
                flow = _pil_to_flowable(pil, img_w, img_h, args.jpeg_quality,
                                        args.shot_px)
                cells.append((flow, caption))
            rows = []
            for i in range(0, len(cells), 2):
                pair = cells[i:i + 2]
                rows.append([c[0] for c in pair])
                rows.append([Paragraph(esc(c[1]), styles["caption"]) for c in pair])
            widths = [img_w] * min(2, len(cells))
            at = Table(rows, colWidths=widths)
            at.setStyle(TableStyle([
                ("VALIGN", (0, 0), (-1, -1), "TOP"),
                ("ALIGN", (0, 0), (-1, -1), "CENTER"),
                ("TOPPADDING", (0, 0), (-1, -1), 3),
                ("BOTTOMPADDING", (0, 0), (-1, -1), 8),
            ]))
            head_txt = "Attachments — screen-by-screen evidence"
            if b.get("diff_boxes"):
                head_txt += " (red outline = what changed on screen)"
            elif b.get("no_screen_change"):
                head_txt += " (screens are identical — the action had no effect)"
            story.append(KeepTogether([Paragraph(head_txt, styles["label"]), at]))
        else:
            reason = ("No screenshot could be captured — the app session was no longer "
                      "responding when the failure occurred (frequently a crash signature)."
                      if b["session_dead"] else
                      "No screenshot was captured for this failure (report artifact "
                      "incomplete for this job).")
            story.append(Paragraph(reason, styles["caption"]))

        if i < len(bugs) - 1:
            story.append(PageBreak())

    doc.build(story)


# ── Self-test (driver-free, synthetic artifacts) ──────────────────────────────

# Mirrors the REAL framework output: the first mined row is a "✅ Assertion passed"
# echo, and every failure is logged TWICE — an Info-badged "❌ Assertion failed: …"
# row that carries the moment-of-failure screenshot, then the Fail-badged row.
SELFTEST_HTML = """<html><body><ul>
<li class="test-item" status="fail" test-id="1" tag="Issues">
<div class="test-detail"><p class="name">TC_SELF_001 - Verify widget saves correctly</p></div>
<div class="test-contents"><table><tbody>
<tr class="event-row"><td><span class="badge log info-bg">Info</span></td><td>1:00:00 PM</td>
<td>&#128248; Initial state<div class="eg-shot"><img src="data:image/jpeg;base64,%B64%"/></div></td></tr>
<tr class="event-row"><td><span class="badge log info-bg">Info</span></td><td>1:00:01 PM</td>
<td>&#9989; Assertion passed: Widgets screen opens from the dashboard</td></tr>
<tr class="event-row"><td><span class="badge log info-bg">Info</span></td><td>1:00:01 PM</td>
<td>Step 1: Navigate to Widgets</td></tr>
<tr class="event-row"><td><span class="badge log info-bg">Info</span></td><td>1:00:02 PM</td>
<td>Widget count: 0</td></tr>
<tr class="event-row"><td><span class="badge log info-bg">Info</span></td><td>1:00:02 PM</td>
<td>Step 2: Tap Save<div class="eg-shot"><img src="data:image/jpeg;base64,%B64%"/></div></td></tr>
<tr class="event-row"><td><span class="badge log info-bg">Info</span></td><td>1:00:03 PM</td>
<td>&#10060; Assertion failed: Widget must be saved - Expected: Saved, Actual: Draft<div class="eg-shot"><img src="data:image/jpeg;base64,%B64%"/></div></td></tr>
<tr class="event-row"><td><span class="badge log fail-bg">Fail</span></td><td>1:00:03 PM</td>
<td>&#10060; Widget must be saved - Expected: Saved, Actual: Draft</td></tr>
</tbody></table></div></li></ul></body></html>"""

# TC_SELF_002 is data-driven: it FAILS for 'Fuse' and PASSES for 'Cable'. A
# method-level collapse would report it as a pass and lose a real defect.
SELFTEST_XML = """<?xml version="1.0" encoding="UTF-8"?>
<testng-results skipped="0" failed="2" total="3" passed="1">
<suite name="S"><test name="T"><class name="com.egalvanic.tests.SelfTest">
<test-method signature="TC_SELF_001_verifyWidgetSaves()" name="TC_SELF_001_verifyWidgetSaves"
 duration-ms="1234" status="FAIL" started-at="2026-01-01T00:00:00Z" finished-at="2026-01-01T00:00:01Z">
<exception class="java.lang.AssertionError"><message><![CDATA[Widget must be saved - Expected: Saved, Actual: Draft]]></message>
<full-stacktrace><![CDATA[java.lang.AssertionError: Widget must be saved
\tat com.egalvanic.base.BaseTest.assertTrue(BaseTest.java:1)]]></full-stacktrace></exception>
</test-method>
<test-method signature="TC_SELF_002_classContract()" name="TC_SELF_002_classContract"
 duration-ms="10" status="FAIL" started-at="2026-01-01T00:00:00Z" finished-at="2026-01-01T00:00:01Z">
<params><parameter index="0"><value><![CDATA[Fuse]]></value></parameter>
<parameter index="1"><value><![CDATA[[Ljava.lang.String;@1f010bf0]]></value></parameter></params>
<exception class="java.lang.AssertionError"><message><![CDATA[Engineering block must render - Expected: visible, Actual: missing]]></message>
<full-stacktrace><![CDATA[java.lang.AssertionError: Engineering block must render]]></full-stacktrace></exception>
</test-method>
<test-method signature="TC_SELF_002_classContract()" name="TC_SELF_002_classContract"
 duration-ms="10" status="PASS" started-at="2026-01-01T00:00:00Z" finished-at="2026-01-01T00:00:01Z">
<params><parameter index="0"><value><![CDATA[Cable]]></value></parameter>
<parameter index="1"><value><![CDATA[[Ljava.lang.String;@40db2a24]]></value></parameter></params>
</test-method>
</class></test></suite></testng-results>"""


def run_selftest(args) -> int:
    import tempfile
    from PIL import Image as PILImage
    failures = []

    def check(cond, msg):
        (failures.append(msg) if not cond else None)
        print(("  ✅ " if cond else "  ❌ ") + msg)

    with tempfile.TemporaryDirectory() as td:
        art = os.path.join(td, "results", "selftest-report")
        os.makedirs(os.path.join(art, "target", "surefire-reports"))
        os.makedirs(os.path.join(art, "reports", "detailed"))
        os.makedirs(os.path.join(art, "screenshots"))
        buf = io.BytesIO()
        PILImage.new("RGB", (60, 120), (200, 40, 40)).save(buf, format="JPEG")
        b64 = base64.b64encode(buf.getvalue()).decode()
        with open(os.path.join(art, "reports", "detailed", "Detailed_Report_1.html"), "w") as fh:
            fh.write(SELFTEST_HTML.replace("%B64%", b64))
        with open(os.path.join(art, "target", "surefire-reports", "testng-results.xml"), "w") as fh:
            fh.write(SELFTEST_XML)
        PILImage.new("RGB", (60, 120), (40, 40, 200)).save(
            os.path.join(art, "screenshots",
                         "TC_SELF_001_verifyWidgetSaves_FAILED_20260101_000001.png"))

        primary, rerun = parse_methods(os.path.join(td, "results"), "failures-rerun", set())
        by_name = {(m["method"], m["params"]): m for m in primary}
        check(by_name[("TC_SELF_001_verifyWidgetSaves", "")]["status"] == "FAIL",
              "XML parse finds the FAIL")
        check(by_name[("TC_SELF_001_verifyWidgetSaves", "")]["exc_msg"]
              .startswith("Widget must be saved"),
              "exception message extracted from <exception>")
        check(by_name[("TC_SELF_002_classContract", "Fuse")]["status"] == "FAIL"
              and by_name[("TC_SELF_002_classContract", "Cable")]["status"] == "PASS",
              "data-driven invocations keyed separately (a passing case cannot "
              "mask a failing one)")
        check(all(not _UNSTABLE_PARAM.match(p) for (_m, p) in by_name),
              "unstable object-identity params excluded from the case signature")
        merged = merge_rerun(primary, rerun)
        fails = [m for m in merged if m["status"] == "FAIL"]
        check(len(fails) == 2, "both failing invocations survive the merge")
        # compose_bug consumes MERGED records (only those carry evidence_roots).
        by_name = {(m["method"], m["params"]): m for m in merged}

        class A:  # minimal args stand-in
            src = ""
            run_id = "123456789"
            max_steps = 30
        bug = compose_bug(by_name[("TC_SELF_001_verifyWidgetSaves", "")], A, [], 1)
        check(bug["tcid"] == "TC_SELF_001", "TC id derived from method name")
        check(any("Navigate to Widgets" in s for s in bug["steps"]),
              "steps mined from detailed report")
        check(not any("must be saved" in s for s in bug["steps"]),
              "assertion-FAILURE text never becomes a reproduction step "
              "(Info-badged ❌ twin row)")
        check(any("Widgets screen opens" in s for s in bug["steps"]),
              "first mined step is not dropped by the ✅ dedup guard")
        check(not any(s.startswith("Widget count:") for s in bug["steps"]),
              "diagnostic value echoes filtered out of steps")
        check("Saved" in bug["expected"] and "Draft" in bug["actual"],
              "Expected/Actual parsed from assert message")
        check(bug["png"] is not None, "failure PNG located on disk")
        check({s["role"] for s in bug["shots"]} >= {"context", "action", "failure"},
              "before / action / failure screenshot narrative captured")
        check(all(s["caption"] for s in bug["shots"]),
              "every screenshot carries the step it belongs to as its caption")
        check(bug["title"].startswith("["), "title carries [Area] prefix")

        case_bug = compose_bug(by_name[("TC_SELF_002_classContract", "Fuse")], A, [], 2)
        check("[case: Fuse]" in case_bug["title"], "failing data case named in the title")
        check(any("Fuse" in s for s in case_bug["steps"]),
              "failing data case named in the steps")

        # Plain-English issue statements — the customer must never have to read
        # an assertion to learn what broke.
        check(bug["issue_text"] and "assert" not in bug["issue_text"].lower(),
              "plain-English issue statement produced")
        cases = [
            (dict(fail_text="Save Changes button should be visible after edit",
                  no_screen_change=True, last_action="Step 4: Tap Save Changes"),
             "unresponsive", "Save Changes"),
            (dict(fail_text="Validation error message should be shown for empty name",
                  expected="required", actual=""), "validation", None),
            (dict(fail_text="Room must be saved successfully", actual="Draft"),
             "not_saved", None),
            (dict(fail_text="Asset row tap must open the asset editor full-screen"),
             "navigation", None),
            (dict(fail_text="no asset cell starting with 'Ns' on the Assets list"),
             "missing", None),
        ]
        check(extract_control("", "", "Clicking Save Changes button directly")
              == "Save Changes button",
              "control name keeps its own words when the leading verb is stripped")
        for kwargs, want_key, want_ctrl in cases:
            k, txt = classify_issue(kwargs.pop("fail_text"), "", "TC_X_001", [],
                                    kwargs.pop("expected", ""), kwargs.pop("actual", ""),
                                    **kwargs)
            ok = (k == want_key) and bool(txt) and (not want_ctrl or want_ctrl in txt)
            check(ok, f"issue classified as '{want_key}' → {txt[:66] or '(empty)'}")

        # Pixel-identical before/after is the proof a control did nothing.
        from PIL import Image as PILImage, ImageDraw as PILDraw
        base = PILImage.new("RGB", (120, 240), (250, 250, 252))
        same_boxes, same_frac = diff_regions(base, base.copy())
        moved = base.copy()
        PILDraw.Draw(moved).rectangle([10, 90, 110, 150], fill=(200, 30, 30))
        moved_boxes, moved_frac = diff_regions(base, moved)
        check(not same_boxes and same_frac < 0.0004,
              "identical screens report no change (control-did-nothing proof)")
        check(moved_boxes and moved_frac > 0.0004,
              "changed region detected and boxed for highlighting")
        banner = annotate(base, "The Save button did not respond", moved_boxes)
        check(banner.size == base.size and list(banner.getdata()) != list(base.getdata()),
              "annotation draws the issue banner and highlight onto the screenshot")

        # An overrides file with an out-of-range severity must not kill the report.
        odd = compose_bug(fails[0], A,
                          [{"match": "TC_SELF", "severity": "Critical",
                            "priority": "P0 <b>", "note": "x"}], 3)
        check(odd["severity"] in SEV_COLOR and odd["priority"] in SEV_COLOR,
              "unknown override severity/priority normalized instead of crashing")

        out = os.path.join(td, "out.pdf")
        ns = argparse.Namespace(**vars(args))
        ns.out = out
        ns.run_id = "123456789"
        build_pdf([bug, case_bug, odd],
                  {"total": 2, "passed": 0, "failed": 2, "skipped": 0, "rate": 0.0}, ns)
        check(os.path.isfile(out) and os.path.getsize(out) > 5000, "PDF written")

        # Jira CSV round-trip: every bug becomes one importable row with the
        # template sections intact in the description.
        import csv as _csv
        jpath = os.path.join(td, "jira.csv")
        emit_jira_csv([bug, case_bug], ns, jpath)
        with open(jpath, encoding="utf-8-sig") as fh:
            rows = list(_csv.DictReader(fh))
        check(len(rows) == 2 and rows[0]["Issue Type"] == "Bug",
              "Jira CSV has one Bug row per defect")
        d = rows[0]["Description"]
        check(all(h in d for h in ("h3. Steps to Reproduce", "h3. Actual Result",
                                   "h3. Expected Result", "h3. Preconditions")),
              "Jira description carries the full bug template sections")
        check(rows[0]["Summary"].startswith("[") and rows[0]["Severity"] in SEV_COLOR,
              "Jira summary/severity populated from the composed bug")

    print(f"\nself-test: {'PASS' if not failures else 'FAIL'} "
          f"({len(failures)} failing checks)")
    return 1 if failures else 0


# ── Main ──────────────────────────────────────────────────────────────────────

def main():
    ap = argparse.ArgumentParser(description=__doc__,
                                 formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("--results", help="artifact root dir (all-reports/)")
    ap.add_argument("--out", default="reports/Bug_Report.pdf")
    ap.add_argument("--rerun-key", default="failures-rerun",
                    help="module-key substring identifying rerun artifacts")
    ap.add_argument("--exclude", default="", help="comma-separated module substrings to skip")
    ap.add_argument("--src", default="src/test/java", help="test source root for steps fallback")
    ap.add_argument("--overrides", default=".github/scripts/bug-report-overrides.json")
    ap.add_argument("--run-url", default=os.environ.get("RUN_URL", ""))
    ap.add_argument("--run-id", default=os.environ.get("GITHUB_RUN_ID", ""))
    ap.add_argument("--run-date", default=datetime.now(timezone.utc).strftime("%Y-%m-%d"))
    ap.add_argument("--app-plist", default="apps/Z Platform-QA.app/Info.plist")
    ap.add_argument("--app-version", default="")
    ap.add_argument("--env-name", default="QA")
    ap.add_argument("--platform", default="iOS (XCUITest automation)")
    ap.add_argument("--device", default="iPhone 16 Pro (Simulator)")
    ap.add_argument("--ios-version", default=os.environ.get("PLATFORM_VERSION", "18.x"))
    ap.add_argument("--repro-note", default="",
                    help="reproducibility text for non-reran fails (e.g. the rerun "
                         "workflow, where the shards ARE the primary run)")
    ap.add_argument("--max-steps", type=int, default=30)
    ap.add_argument("--max-shots", type=int, default=4,
                    help="screenshots per defect (before / action / failure / full-res)")
    ap.add_argument("--jpeg-quality", type=int, default=56)
    ap.add_argument("--shot-px", type=int, default=700,
                    help="max screenshot edge in px (report size vs zoom detail)")
    ap.add_argument("--include-stack", action="store_true", default=True,
                    help="(default) include the technical stack excerpt")
    ap.add_argument("--no-include-stack", action="store_false", dest="include_stack",
                    help="omit the technical stack excerpt from each bug page")
    ap.add_argument("--limit", type=int, default=0, help="cap bug count (0 = all)")
    ap.add_argument("--summary-json", default="", help="write machine summary here")
    ap.add_argument("--jira-csv", default="",
                    help="also write a Jira-importable CSV (one Bug row per defect) here")
    ap.add_argument("--selftest", action="store_true")
    args = ap.parse_args()

    # reportlab / pillow are the only non-stdlib deps
    try:
        import reportlab  # noqa: F401
        from PIL import Image  # noqa: F401
    except ImportError:
        subprocess.run([sys.executable, "-m", "pip", "install", "--quiet",
                        "--break-system-packages", "reportlab", "pillow"], check=False)
        try:
            import reportlab  # noqa: F401
        except ImportError:
            subprocess.run([sys.executable, "-m", "pip", "install", "--quiet",
                            "reportlab", "pillow"])

    if args.selftest:
        sys.exit(run_selftest(args))

    if not args.results:
        ap.error("--results is required (or use --selftest)")

    if not args.app_version:
        args.app_version = detect_app_version(args.app_plist)

    overrides = []
    if args.overrides and os.path.isfile(args.overrides):
        try:
            overrides = json.load(open(args.overrides))
        except Exception as e:
            print(f"  ! overrides ignored: {e}", file=sys.stderr)

    exclude = {e.strip() for e in args.exclude.split(",") if e.strip()}
    print(f"Parsing TestNG results under {args.results} …")
    primary, rerun = parse_methods(args.results, args.rerun_key, exclude)
    merged = merge_rerun(primary, rerun)

    # Headline numbers are METHOD-level and use the same denominator as
    # ios_client_report.py (passed/total), so the PDF and the client report
    # attached to the same email never disagree. Bug entries below are
    # INVOCATION-level, so a data-driven test that fails for 4 of 38 cases
    # still yields 4 documented defects.
    by_method = {}
    for m in merged:
        k = (m["cls"], m["method"])
        prev = by_method.get(k)
        rank = {"PASS": 2, "FAIL": 1, "SKIP": 0}
        if prev is None or rank[m["status"]] > rank[prev]:
            by_method[k] = m["status"]
    total = len(by_method)
    passed = sum(1 for s in by_method.values() if s == "PASS")
    failed = sum(1 for s in by_method.values() if s == "FAIL")
    skipped = total - passed - failed
    stats = {"total": total, "passed": passed, "failed": failed, "skipped": skipped,
             "rate": (passed / total * 100.0) if total else 0.0}
    print(f"  {total} tests | {passed} pass | {failed} FAIL | {skipped} skip")

    fails = sorted((m for m in merged if m["status"] == "FAIL"),
                   key=lambda m: (area_for(m["module"], m["cls"]), m["cls"], m["method"]))
    if args.limit:
        fails = fails[:args.limit]

    bugs = []
    for i, rec in enumerate(fails, 1):
        bugs.append(compose_bug(rec, args, overrides, i))
        if i % 25 == 0:
            print(f"  … composed {i}/{len(fails)} bug entries")

    src_counts = defaultdict(int)
    for b in bugs:
        src_counts[b["steps_source"]] += 1
    print(f"  steps source: {dict(src_counts)}")
    with_shot = sum(1 for b in bugs if b["png"] or b["shots"])
    annotated = sum(1 for b in bugs if b["diff_boxes"])
    proven_dead = sum(1 for b in bugs if b["no_screen_change"])
    classified = sum(1 for b in bugs if b["issue_text"])
    print(f"  {with_shot}/{len(bugs)} bugs have screenshots "
          f"({annotated} with change-highlight boxes, "
          f"{proven_dead} proven unchanged after the action)")
    print(f"  {classified}/{len(bugs)} carry a plain-English issue statement")

    if args.jira_csv:
        emit_jira_csv(bugs, args, args.jira_csv)

    os.makedirs(os.path.dirname(os.path.abspath(args.out)), exist_ok=True)
    print(f"Rendering PDF → {args.out} …")
    build_pdf(bugs, stats, args)
    size_mb = os.path.getsize(args.out) / 1048576.0
    print(f"  wrote {args.out} ({size_mb:.1f} MB, {len(bugs)} defects)")

    if args.summary_json:
        json.dump({"bugs": len(bugs), "pdf": args.out, "size_mb": round(size_mb, 2),
                   "with_screenshots": with_shot, "stats": stats},
                  open(args.summary_json, "w"), indent=2)


if __name__ == "__main__":
    main()
