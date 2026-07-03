#!/usr/bin/env python3
"""
QA test-data cleanup for the automation site ("(s) Wild Goose Brewery").

WHY: months of CI runs piled up automation debris that made BOTH QA and CI slow —
audited 2026-07-03: 384 live buildings, 397 floors, 727 tasks, 82 work orders and
(before the first cleanup) 1,070 junk assets on the automation site. That bloat IS
the giant-DOM/WDA-wedge root cause for the Locations tree and Work Orders screens,
and it inflates every /sld/v3 sync payload.

WHAT IT DOES: soft-deletes (is_deleted=true — reversible) rows that are UNAMBIGUOUSLY
automation-created, using the app's own update endpoints:
  nodes:       PUT /node/update/{id}
  buildings:   PUT /location/building/{id}
  floors:      PUT /location/floor/{id}
  rooms:       PUT /location/room/{id}
  tasks:       PUT /task/update/{id}
  ir_sessions: PUT /ir_session/update/{id}

SAFETY RAILS
  - Only the automation SLD (hard-coded id) — never touches other sites.
  - Only names matching strict automation signatures (13-digit epoch tails and the
    known Test*/MultiSync/DoubleTapTest/... families).
  - Work orders: only INACTIVE ones from BEFORE the current month; active or
    human-named sessions are never touched.
  - Every deletion appended to an undo log (JSONL of id+collection); undo = PUT the
    same endpoint with {"is_deleted": false}.
  - DRY_RUN=1 (default when env unset in CI) prints what WOULD be deleted.

Usage:
  DRY_RUN=1 python3 .github/scripts/qa-data-cleanup.py     # audit only
  DRY_RUN=0 python3 .github/scripts/qa-data-cleanup.py     # actually clean
Credentials: QA_API_EMAIL / QA_API_PASSWORD env vars (defaults = automation creds).
"""
import json
import os
import re
import sys
import time
import urllib.request

BASE = os.environ.get("QA_API_BASE", "https://api.qa.egalvanic.ai/api")
SLD_ID = os.environ.get("QA_CLEANUP_SLD", "9138fd14-a3c9-495a-b086-6ef520f92168")
DRY_RUN = os.environ.get("DRY_RUN", "1") != "0"
UNDO_LOG = os.environ.get("UNDO_LOG", "qa-cleanup-undo.jsonl")


def _creds():
    """Credentials: env vars first (GitHub secrets in CI); fallback = parse the
    QA automation account from AppConstants.java so the value lives in exactly
    one place in this repo (no duplicated literals in this script)."""
    email = os.environ.get("QA_API_EMAIL") or ""
    password = os.environ.get("QA_API_PASSWORD") or ""
    if email and password:
        return email, password
    consts = os.path.join(os.path.dirname(__file__), "..", "..",
                          "src/main/java/com/egalvanic/constants/AppConstants.java")
    try:
        src = open(consts, encoding="utf-8").read()
        email = email or re.search(r'VALID_EMAIL\s*=\s*"([^"]+)"', src).group(1)
        password = password or re.search(r'VALID_PASSWORD\s*=\s*"([^"]+)"', src).group(1)
        return email, password
    except Exception:
        sys.exit("No credentials: set QA_API_EMAIL/QA_API_PASSWORD or run from the repo "
                 "(AppConstants.java fallback not found)")

EPOCH = r"1[678]\d{11}"  # 13-digit ms epoch (2023-2027)

RULES = {
    # collection: (update_path_fmt, name_keys, junk_regex)
    "nodes": ("/node/update/{id}", ("label",),
              re.compile(rf"({EPOCH}\s*$|^(TestAsset|Asset_Verify|Asset_NoPhoto|DupTest|SearchCaseTest|PartialSearchTest|CancelTest|LinkTest|QRTest|EditQRTest|NoSubtype|PERSISTED|Trim)\w*[_ -]?\d+\s*$)", re.IGNORECASE)),
    "buildings": ("/location/building/{id}", ("name", "label"),
                  re.compile(rf"({EPOCH}|^(MultiSync|Test ?Building|DoubleTapTest|TestDelete|Sync[_ ]|TestBldg|BuildingCount|History Test))", re.IGNORECASE)),
    "floors": ("/location/floor/{id}", ("name", "label"),
               re.compile(rf"({EPOCH}|^(TestFloor|Floor_\d|FloorCount|Optional Notes|TestDelete|TestRoom))", re.IGNORECASE)),
    "rooms": ("/location/room/{id}", ("name", "label"),
              re.compile(rf"({EPOCH}|^(Room_\d|RoomCnt|RoomCount|CountTest|TestDelete))", re.IGNORECASE)),
    "tasks": ("/task/update/{id}", ("name", "title", "label"),
              re.compile(rf"({EPOCH}|^Arc Flash Audit - Work Order - (Jan|Feb|Mar|Apr|May|Jun)\b)", re.IGNORECASE)),
}
# ir_sessions handled specially (active/date guards)
SESSION_PATH = "/ir_session/update/{id}"
OLD_WO = re.compile(r"^(Work Order|Job) - (Jan|Feb|Mar|Apr|May|Jun)\b", re.IGNORECASE)


def req(path, method="GET", body=None, token=None, raw=False):
    r = urllib.request.Request(BASE + path, method=method)
    r.add_header("Content-Type", "application/json")
    r.add_header("X-Subdomain", os.environ.get("QA_API_SUBDOMAIN", "acme"))
    r.add_header("X-Language", "en")
    if token:
        r.add_header("Authorization", "Bearer " + token)
    data = json.dumps(body).encode() if body is not None else None
    try:
        with urllib.request.urlopen(r, data=data, timeout=120) as resp:
            t = resp.read().decode()
            return (resp.status, t if raw else (json.loads(t) if t else {}))
    except urllib.error.HTTPError as e:
        return (e.code, e.read().decode()[:200])


def main():
    print(("DRY RUN — nothing will be deleted" if DRY_RUN else "LIVE — soft-deleting") + f" (site {SLD_ID[:8]}…)")
    email, password = _creds()
    code, login = req("/auth/v2/login", "POST",
                      {"email": email, "password": password, "subdomain": os.environ.get("QA_API_SUBDOMAIN", "acme")},
                      raw=True)
    if code != 200:
        sys.exit(f"login failed: HTTP {code}")
    token = re.search(r'"access_token"\s*:\s*"([^"]+)"', login).group(1)

    code, det = req(f"/sld/v3/{SLD_ID}", token=token)
    if code != 200:
        sys.exit(f"GET /sld/v3 failed: HTTP {code}")
    d = det.get("data") or det

    # Never delete rows created in the last 48h: a CI run may be IN FLIGHT using
    # fixtures it just created (epoch-named, so they match the junk patterns).
    # FAIL-SAFE: an unparseable timestamp counts as recent (skip). Recency is
    # checked from BOTH the row's created_at/date_created AND the 13-digit epoch
    # embedded in the automation name itself (junk names carry their birth time).
    import datetime
    cutoff_dt = datetime.datetime.now(datetime.timezone.utc) - datetime.timedelta(hours=48)
    cutoff_ms = cutoff_dt.timestamp() * 1000

    def too_recent(r, name):
        ts = r.get("created_at") or r.get("date_created")
        if ts:
            try:
                parsed = datetime.datetime.fromisoformat(str(ts).replace("Z", "+00:00"))
                if parsed.tzinfo is None:
                    parsed = parsed.replace(tzinfo=datetime.timezone.utc)
                if parsed >= cutoff_dt:
                    return True
            except (ValueError, TypeError):
                return True  # fail-safe: can't prove it's old -> keep it
        m = re.search(EPOCH, name or "")
        if m and float(m.group(0)) >= cutoff_ms:
            return True
        return False

    undo = None if DRY_RUN else open(UNDO_LOG, "a")
    grand_ok = grand_fail = 0
    for coll, (path_fmt, name_keys, junk) in RULES.items():
        rows = [r for r in (d.get(coll) or []) if not r.get("is_deleted")]
        targets = []
        skipped_recent = 0
        for r in rows:
            nm = next((str(r[k]) for k in name_keys if r.get(k)), "").strip()
            if nm and junk.search(nm):
                if too_recent(r, nm):
                    skipped_recent += 1
                    continue
                targets.append((r["id"], nm))
        if skipped_recent:
            print(f"             ({skipped_recent} recent rows kept — possible in-flight CI fixtures)")
        print(f"{coll:12s} live={len(rows):5d}  junk-targets={len(targets):5d}")
        if DRY_RUN:
            for _, nm in targets[:5]:
                print("     would delete:", nm[:60])
            continue
        ok = fail = 0
        for nid, nm in targets:
            c, _ = req(path_fmt.format(id=nid), "PUT", {"is_deleted": True}, token=token, raw=True)
            if c == 200:
                ok += 1
                undo.write(json.dumps({"collection": coll, "id": nid, "name": nm}) + "\n")
            else:
                fail += 1
            time.sleep(0.05)
        undo.flush()
        grand_ok += ok; grand_fail += fail
        print(f"             deleted={ok} failed={fail}")

    # Work orders: inactive + old-month named only
    sessions = [s for s in (d.get("ir_sessions") or []) if not s.get("is_deleted")]
    targets = [(s["id"], s.get("name") or "") for s in sessions
               if not s.get("active") and OLD_WO.match((s.get("name") or "").strip())]
    print(f"{'ir_sessions':12s} live={len(sessions):5d}  junk-targets={len(targets):5d} (inactive, pre-July only)")
    if not DRY_RUN:
        ok = fail = 0
        for sid_, nm in targets:
            c, _ = req(SESSION_PATH.format(id=sid_), "PUT", {"is_deleted": True}, token=token, raw=True)
            if c == 200:
                ok += 1
                undo.write(json.dumps({"collection": "ir_sessions", "id": sid_, "name": nm}) + "\n")
            else:
                fail += 1
            time.sleep(0.05)
        undo.flush(); undo.close()
        grand_ok += ok; grand_fail += fail
        print(f"             deleted={ok} failed={fail}")
        print(f"\nTOTAL soft-deleted={grand_ok} failed={grand_fail}  undo-log={UNDO_LOG}")
    else:
        for _, nm in targets[:5]:
            print("     would delete:", nm[:60])


if __name__ == "__main__":
    main()
