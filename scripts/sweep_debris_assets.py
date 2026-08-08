#!/usr/bin/env python3
"""Sweep auto-generated debris ASSETS (nodes) from a QA site.

Why: test runs create timestamped junk assets (NoSubtype_*, PERSISTED_*,
DupTest_*, '(copy N)' dupes, ...). They accumulate until fixtures scroll out
of reach and whole modules fail on 'no asset cell starting with X'
(run 31214326457: 60-fail AssetEngineer cluster; the Assets list had 124
live nodes, 67 of them debris). Soft-delete works for nodes via
PUT /node/update/{id} {"is_deleted": true} + x-direct-write (verified live
2026-08-08; unlike ir_session, the node update applies).

Usage:
  python3 scripts/sweep_debris_assets.py [--sld-id <id>] [--dry-run]

Defaults to the Wild Goose Brewery QA SLD. NEVER touches names outside the
debris patterns below.
"""
import argparse, json, os, re, sys, urllib.request

BASE = "https://api.qa.egalvanic.ai/api"
DEFAULT_SLD = "9138fd14-a3c9-495a-b086-6ef520f92168"  # (s) Wild Goose Brewery
# Env-first; the fallbacks mirror the QA login the suite already uses
# (AppConstants.VALID_EMAIL/VALID_PASSWORD) so CI needs no new secrets.
EMAIL = os.environ.get("SWEEP_EMAIL", "abhiyant.singh+admin@egalvanic.com")
PASSWORD = os.environ.get("SWEEP_PASSWORD", "RP@egalvanic123")
SUBDOMAIN = os.environ.get("SWEEP_SUBDOMAIN", "acme")

DEBRIS_PREFIX = re.compile(r"^(NoSubtype_|PERSISTED_|E2E_|DEL_|Dup_|DupTest_|ClassChange_|Rename_|Asset_Verify|CaseTest_|QRTest|QRLenTest_|EditQRTest)")
DEBRIS_TAIL = re.compile(r"\(copy( \d+)?\)$")
DEBRIS_TS = re.compile(r"_\d{13}")


def call(method, path, token=None, body=None):
    req = urllib.request.Request(
        BASE + path,
        data=json.dumps(body).encode() if body is not None else None,
        headers={"Content-Type": "application/json", "X-Subdomain": SUBDOMAIN,
                 **({"Authorization": f"Bearer {token}", "x-direct-write": "true"} if token else {})},
        method=method)
    with urllib.request.urlopen(req, timeout=30) as r:
        return json.loads(r.read().decode())


def is_debris(name):
    return bool(DEBRIS_PREFIX.match(name) or DEBRIS_TAIL.search(name) or DEBRIS_TS.search(name))


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--sld-id", default=DEFAULT_SLD)
    ap.add_argument("--dry-run", action="store_true")
    args = ap.parse_args()

    login = call("POST", "/auth/v2/login", body={"email": EMAIL, "password": PASSWORD, "subdomain": SUBDOMAIN})
    token = login.get("token") or login.get("access_token") or login.get("data", {}).get("token")
    sld = call("GET", f"/sld/v3/{args.sld_id}", token=token)
    nodes = sld.get("data", {}).get("nodes") or sld.get("nodes") or []
    live = [n for n in nodes if str(n.get("is_deleted")) in ("False", "false", "None")]
    targets = [(n["id"], n.get("label") or "") for n in live if is_debris(n.get("label") or "")]
    print(f"live={len(live)} debris={len(targets)} keepers={len(live)-len(targets)}")
    if args.dry_run:
        for _, label in targets:
            print("  would delete:", label)
        return
    ok = 0
    for nid, label in targets:
        try:
            call("PUT", f"/node/update/{nid}", token=token, body={"id": nid, "is_deleted": True})
            ok += 1
        except Exception as e:
            print("  FAILED:", label, e, file=sys.stderr)
    print(f"deleted {ok}/{len(targets)}")


if __name__ == "__main__":
    main()
