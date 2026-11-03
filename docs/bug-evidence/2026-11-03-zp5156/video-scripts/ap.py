#!/usr/bin/env python3
"""Tiny Appium W3C client for manual repro. Session id kept in sid.txt next to this file."""
import json, sys, os, base64, time, urllib.request, re
HERE = os.path.dirname(os.path.abspath(__file__))
BASE = 'http://127.0.0.1:4723'
SIDF = os.path.join(HERE, 'sid.txt')
def call(method, path, body=None, timeout=300):
    data = json.dumps(body).encode() if body is not None else None
    req = urllib.request.Request(BASE + path, data=data, method=method, headers={'Content-Type': 'application/json'})
    try:
        with urllib.request.urlopen(req, timeout=timeout) as r:
            return json.loads(r.read() or b'{}')
    except urllib.error.HTTPError as e:
        return json.loads(e.read() or b'{}')
def sid(): return open(SIDF).read().strip()
def s(path): return '/session/' + sid() + path
def tap(x, y):
    return call('POST', s('/actions'), {'actions': [{'type': 'pointer', 'id': 'f', 'parameters': {'pointerType': 'touch'},
        'actions': [{'type': 'pointerMove', 'duration': 0, 'x': int(x), 'y': int(y)}, {'type': 'pointerDown', 'button': 0},
                    {'type': 'pause', 'duration': 80}, {'type': 'pointerUp', 'button': 0}]}]})
def swipe(x1, y1, x2, y2, ms=400):
    return call('POST', s('/actions'), {'actions': [{'type': 'pointer', 'id': 'f', 'parameters': {'pointerType': 'touch'},
        'actions': [{'type': 'pointerMove', 'duration': 0, 'x': int(x1), 'y': int(y1)}, {'type': 'pointerDown', 'button': 0},
                    {'type': 'pause', 'duration': 150}, {'type': 'pointerMove', 'duration': ms, 'x': int(x2), 'y': int(y2)},
                    {'type': 'pointerUp', 'button': 0}]}]})
def longpress(x, y, ms=1200):
    return call('POST', s('/actions'), {'actions': [{'type': 'pointer', 'id': 'f', 'parameters': {'pointerType': 'touch'},
        'actions': [{'type': 'pointerMove', 'duration': 0, 'x': int(x), 'y': int(y)}, {'type': 'pointerDown', 'button': 0},
                    {'type': 'pause', 'duration': ms}, {'type': 'pointerUp', 'button': 0}]}]})
def source(): return call('GET', s('/source'))['value']
def nodes(src=None):
    src = src or source()
    out = []
    for m in re.finditer(r'<(XCUIElementType\w+)([^>]*)>', src):
        a = dict(re.findall(r'(\w+)="([^"]*)"', m.group(2)))
        if a.get('visible') == 'false': continue
        lab = a.get('label') or a.get('name') or a.get('value') or ''
        if not lab: continue
        out.append((m.group(1).replace('XCUIElementType', ''), lab, int(a.get('x', 0)), int(a.get('y', 0)), int(a.get('width', 0)), int(a.get('height', 0)), a.get('value', '')))
    return out
if __name__ == '__main__':
    cmd = sys.argv[1]
    if cmd == 'start':
        env = json.loads(sys.argv[2]) if len(sys.argv) > 2 else {}
        caps = {'platformName': 'iOS', 'appium:automationName': 'XCUITest', 'appium:udid': 'B9AA2053-964E-4635-BC9F-9B6CB9052D25',
                'appium:bundleId': 'com.egalvanic.zplatform-QA', 'appium:noReset': True, 'appium:newCommandTimeout': 3600,
                'appium:wdaLaunchTimeout': 240000, 'appium:autoAcceptAlerts': False, 'appium:snapshotMaxDepth': 60,
                'appium:forceAppLaunch': True, 'appium:shouldTerminateApp': True,
                'appium:processArguments': {'env': env}}
        r = call('POST', '/session', {'capabilities': {'alwaysMatch': caps}}, timeout=900)
        v = r.get('value', {})
        if 'sessionId' in v:
            open(SIDF, 'w').write(v['sessionId']); print('session', v['sessionId'])
        else: print(json.dumps(r)[:2000])
    elif cmd == 'shot':
        png = base64.b64decode(call('GET', s('/screenshot'))['value'])
        open(sys.argv[2], 'wb').write(png); print('saved', sys.argv[2])
    elif cmd == 'nodes':
        flt = sys.argv[2] if len(sys.argv) > 2 else ''
        for n in nodes():
            if flt.lower() in (n[1] + n[0]).lower(): print(n)
    elif cmd == 'src':
        open(sys.argv[2], 'w').write(source()); print('saved')
    elif cmd == 'tap': print(tap(float(sys.argv[2]), float(sys.argv[3])).get('value'))
    elif cmd == 'long': print(longpress(float(sys.argv[2]), float(sys.argv[3])).get('value'))
    elif cmd == 'swipe': print(swipe(*map(float, sys.argv[2:6])).get('value'))
    elif cmd == 'tapl':  # tap first visible node whose label equals/contains arg
        want = sys.argv[2]; exact = len(sys.argv) > 3 and sys.argv[3] == 'exact'
        for n in nodes():
            if (n[1] == want) if exact else (want in n[1]):
                print('tap', n); tap(n[2] + n[4] / 2, n[3] + n[5] / 2); break
        else: print('NOT FOUND', want)
    elif cmd == 'type':
        print(call('POST', s('/actions'), {'actions': [{'type': 'key', 'id': 'k', 'actions': sum([[{'type': 'keyDown', 'value': c}, {'type': 'keyUp', 'value': c}] for c in sys.argv[2]], [])}]}).get('value'))
    elif cmd == 'exec':
        print(call('POST', s('/execute/sync'), {'script': sys.argv[2], 'args': [json.loads(sys.argv[3])] if len(sys.argv) > 3 else []}).get('value'))
    elif cmd == 'quit': print(call('DELETE', s('')))
    elif cmd == 'setfield':  # setfield <1-based TextField index> <text>
        r = call('POST', s('/element'), {'using': '-ios class chain', 'value': '**/XCUIElementTypeTextField[%s]' % sys.argv[2]})
        eid = list(r['value'].values())[0]
        call('POST', s('/element/%s/clear' % eid), {})
        print(call('POST', s('/element/%s/value' % eid), {'text': sys.argv[3]}).get('value'))
    elif cmd == 'setph':  # setph <placeholderValue> <text> : visible TextField by placeholder
        r = call('POST', s('/element'), {'using': '-ios class chain', 'value': '**/XCUIElementTypeTextField[`placeholderValue == "%s" AND visible == 1`]' % sys.argv[2]})
        eid = list(r['value'].values())[0]
        rr = call('GET', s('/element/%s/rect' % eid))['value']; tap(rr['x'] + rr['width'] / 2, rr['y'] + rr['height'] / 2); time.sleep(0.6)
        call('POST', s('/element/%s/clear' % eid), {})
        print(call('POST', s('/element/%s/value' % eid), {'text': sys.argv[3]}).get('value'))
