#!/usr/bin/env python3
"""
Send a per-module test-result email right after that module's tests
finish — instead of waiting for the global send-email job at the end
of the workflow. Lets the user review each module's Detailed report
(via artifact download link) as soon as it's available.

Inputs (env vars):
    SMTP_USER       — Gmail address
    SMTP_PASS       — Gmail app password
    EMAIL_TO        — comma-separated recipients
    RUN_URL         — link to workflow run (artifacts UI)
    BRANCH, ACTOR   — for email body
    MODULE_NAME     — friendly module name (e.g. "issues-phase1")
    JOB_STATUS      — github.job.status / step status: "success" | "failure" | "cancelled" | "skipped" (optional)

Reads local files from CWD:
    reports/detailed/Detailed_Report_*.html  (most recent)
    reports/client/Client_Report_*.html      (most recent)
    target/surefire-reports/testng-results.xml

Notes:
    - Detailed report is NEVER attached; we only embed a download
      link to the workflow artifacts (per project preference).
    - Client report IS attached if present (~30 KB).
    - XML parsing prefers defusedxml when available.
"""
import os
import sys
import glob
import html
import smtplib
import ssl
from email.message import EmailMessage
from email.utils import formatdate

# XXE-safe XML parsing — defusedxml is required (CI step pip-installs it).
# Stdlib xml.etree.ElementTree is vulnerable to XXE / billion-laughs and is
# intentionally NOT used as a fallback.
import defusedxml.ElementTree as ET


def newest(pattern):
    matches = sorted(glob.glob(pattern, recursive=True))
    return matches[-1] if matches else None


def parse_testng(path):
    """Return (passed, failed, skipped, failed_names) or (0,0,0,[]) if unreadable."""
    if not path or not os.path.exists(path):
        return 0, 0, 0, []
    try:
        root = ET.parse(path).getroot()
        passed = int(root.get("passed", 0))
        failed = int(root.get("failed", 0))
        skipped = int(root.get("skipped", 0))
        failed_names = []
        for tm in root.iter("test-method"):
            if tm.get("status") == "FAIL" and tm.get("is-config") != "true":
                failed_names.append(tm.get("name", "unknown"))
        return passed, failed, skipped, failed_names
    except Exception as exc:
        print(f"  ! testng XML parse failed: {exc}", file=sys.stderr)
        return 0, 0, 0, []


def count_screenshots(html_path):
    if not html_path or not os.path.exists(html_path):
        return 0
    try:
        with open(html_path, "r", encoding="utf-8", errors="ignore") as fh:
            return fh.read().count("data:image")
    except Exception:
        return 0


def main():
    user = os.environ.get("SMTP_USER")
    pwd = os.environ.get("SMTP_PASS")
    to = os.environ.get("EMAIL_TO", "")
    module = os.environ.get("MODULE_NAME", "module")
    run_url = os.environ.get("RUN_URL", "")
    branch = os.environ.get("BRANCH", "")
    actor = os.environ.get("ACTOR", "")
    job_status = os.environ.get("JOB_STATUS", "")

    if not user or not pwd or not to:
        print("✗ Missing SMTP_USER / SMTP_PASS / EMAIL_TO — skipping module email")
        return 0  # don't fail the job

    detailed = newest("reports/detailed/Detailed_Report_*.html")
    client = newest("reports/client/Client_Report_*.html")
    xml = newest("target/surefire-reports/testng-results.xml")
    if not xml:
        xml = newest("**/testng-results.xml")

    passed, failed, skipped, failed_names = parse_testng(xml)
    total = passed + failed + skipped
    pass_rate = (passed / total * 100) if total else 0
    shot_count = count_screenshots(detailed)

    if job_status.lower() == "cancelled":
        status_label, header_color = "⏱️ TIMEOUT", "#e67e22"
    elif job_status.lower() == "failure" and total == 0:
        status_label, header_color = "❌ JOB FAILED (no test data)", "#dc3545"
    elif failed == 0 and total > 0:
        status_label, header_color = "✅ PASS", "#28a745"
    elif failed > 0:
        status_label, header_color = "❌ FAIL", "#dc3545"
    else:
        status_label, header_color = "⚠️ NO DATA", "#e67e22"

    subject = f"[{status_label}] iOS {module} — {passed}/{total} passed · Detailed Report ready"

    failed_html = ""
    if failed_names:
        shown = failed_names[:25]
        lis = "".join(f"<li><code>{html.escape(n)}</code></li>" for n in shown)
        more = (f"<li><i>… +{len(failed_names) - 25} more</i></li>"
                if len(failed_names) > 25 else "")
        failed_html = (
            f"<h3 style='color:#dc3545;'>Failed tests ({failed})</h3>"
            f"<ul>{lis}{more}</ul>"
        )

    artifact_url = f"{run_url}#artifacts" if run_url else ""
    detailed_basename = os.path.basename(detailed) if detailed else "Detailed_Report_*.html"
    detailed_size_mb = (os.path.getsize(detailed) / 1048576) if detailed else 0

    if detailed:
        detailed_block = f"""
        <div style='margin:18px 0; padding:18px; background:#fff8e1;
                    border-left:4px solid #f9a825; border-radius:6px;'>
          <h3 style='margin:0 0 8px;'>📥 Download Detailed Report</h3>
          <p style='margin:0 0 8px;'>Full step-by-step report with
             <b>{shot_count} screenshots</b> ({detailed_size_mb:.1f} MB).</p>
          <p style='margin:0;'>
            <a href='{html.escape(artifact_url)}'
               style='display:inline-block; padding:8px 18px;
                      background:#f9a825; color:white;
                      text-decoration:none; border-radius:4px;
                      font-weight:bold;'>Open Workflow Artifacts</a>
          </p>
          <p style='margin:8px 0 0; font-size:12px; color:#666;'>
            Look for artifact <code>{html.escape(module)}-report</code> —
            download the ZIP and open
            <code>{html.escape(detailed_basename)}</code> in a browser.
          </p>
        </div>"""
    else:
        detailed_block = """
        <div style='margin:18px 0; padding:12px; background:#ffebee;
                    border-left:4px solid #dc3545; border-radius:6px;
                    color:#dc3545;'>
          <b>⚠️ No detailed report was produced for this module</b>
          (job may have failed before tests ran).
        </div>"""

    body = f"""<!DOCTYPE html>
    <html><body style="font-family:'Segoe UI',Arial,sans-serif; padding:0; margin:0; background:#f5f5f5;">
      <div style="background:linear-gradient(135deg,{header_color} 0%,#333 100%); color:white; padding:25px; text-align:center;">
        <h1 style="margin:0;font-size:22px;">iOS {html.escape(module)} — Detailed Report</h1>
        <p style="margin:8px 0 0;opacity:0.9;font-size:14px;">{status_label} · {passed}/{total} passed · {pass_rate:.1f}% pass rate</p>
      </div>
      <div style="max-width:800px;margin:20px auto;padding:0 20px;">
        <table style="width:100%;margin-bottom:15px;font-size:13px;">
          <tr><td><b>Branch:</b> {html.escape(branch)}</td><td><b>Triggered by:</b> {html.escape(actor)}</td></tr>
          <tr><td><b>Run:</b> <a href='{html.escape(run_url)}'>View Workflow</a></td><td><b>Module status:</b> {html.escape(job_status or status_label)}</td></tr>
        </table>
        <table border='1' cellpadding='8' style='border-collapse:collapse;width:100%;background:white;font-size:13px;'>
          <tr style='background:#4a90d9;color:white;'><th>Metric</th><th>Count</th></tr>
          <tr><td>Total Tests</td><td align='center'><b>{total}</b></td></tr>
          <tr><td>Passed</td><td align='center' style='color:#28a745;'>{passed}</td></tr>
          <tr><td>Failed</td><td align='center' style='color:#dc3545;'>{failed}</td></tr>
          <tr><td>Skipped</td><td align='center' style='color:#ffc107;'>{skipped}</td></tr>
        </table>
        {failed_html}
        {detailed_block}
        <p style="margin-top:15px;color:#999;font-size:11px;text-align:center;">
          eGalvanic iOS Automation — sent immediately on module completion
        </p>
      </div>
    </body></html>"""

    msg = EmailMessage()
    msg["Subject"] = subject
    msg["From"] = f"iOS Automation <{user}>"
    msg["To"] = to
    msg["Date"] = formatdate(localtime=True)
    msg.set_content("Open in an HTML-capable client to see the report summary.")
    msg.add_alternative(body, subtype="html")

    if client:
        try:
            with open(client, "rb") as fh:
                msg.add_attachment(
                    fh.read(),
                    maintype="text", subtype="html",
                    filename=os.path.basename(client),
                )
        except Exception as exc:
            print(f"  ! Could not attach client report: {exc}", file=sys.stderr)

    try:
        context = ssl.create_default_context()
        with smtplib.SMTP_SSL("smtp.gmail.com", 465, context=context) as smtp:
            smtp.login(user, pwd)
            smtp.send_message(msg)
        print(f"✓ Sent {module} email to {to}: {passed}/{total} ({status_label})")
    except Exception as exc:
        print(f"✗ Failed to send {module} email: {exc}", file=sys.stderr)
        # Don't fail the job because of email delivery
    return 0


if __name__ == "__main__":
    sys.exit(main())
