#!/usr/bin/env python3
"""Tiny adb + uiautomator helper. Usage: ad.py dump [filter] | tap X Y | tapt TEXT [exact] | shot FILE | swipe X1 Y1 X2 Y2 [ms] | back | launch"""
import subprocess, sys, re, time, html

ADB = [__import__('os').path.expanduser('~/Library/Android/sdk/platform-tools/adb'), '-s', 'emulator-5554']
PKG = 'com.egalvanic.pz.qa'


def sh(*a, binary=False):
    r = subprocess.run(ADB + list(a), capture_output=True)
    return r.stdout if binary else r.stdout.decode('utf-8', 'replace')


def nodes():
    for _ in range(3):
        x = sh('exec-out', 'uiautomator', 'dump', '/dev/tty')
        if '<hierarchy' in x:
            break
        time.sleep(1)
    out = []
    for m in re.finditer(r'<node ([^>]*?)/?>', x):
        a = dict(re.findall(r'([\w-]+)="([^"]*)"', m.group(1)))
        t = html.unescape(a.get('text', '')); d = html.unescape(a.get('content-desc', ''))
        b = re.findall(r'\d+', a.get('bounds', '[0,0][0,0]'))
        x1, y1, x2, y2 = map(int, b)
        out.append(dict(text=t, desc=d, cls=a.get('class', '').split('.')[-1], rid=a.get('resource-id', ''),
                        click=a.get('clickable') == 'true', bounds=(x1, y1, x2, y2)))
    return out


def label(n):
    return n['text'] or n['desc']


def tap(x, y):
    sh('shell', 'input', 'tap', str(int(x)), str(int(y)))


def find(txt, exact=False):
    for n in nodes():
        l = label(n)
        if l and ((l == txt) if exact else (txt.lower() in l.lower())):
            return n
    return None


def tapt(txt, exact=False, tries=5):
    for _ in range(tries):
        n = find(txt, exact)
        if n:
            x1, y1, x2, y2 = n['bounds']; tap((x1 + x2) / 2, (y1 + y2) / 2); return n
        time.sleep(1)
    return None


if __name__ == '__main__':
    c = sys.argv[1]
    if c == 'dump':
        f = sys.argv[2].lower() if len(sys.argv) > 2 else ''
        for n in nodes():
            l = label(n)
            if l and f in (l + n['cls'] + n['rid']).lower():
                print(n['cls'], '|', l.replace('\n', ' ')[:90], '|', n['bounds'], 'C' if n['click'] else '', n['rid'].split('/')[-1])
    elif c == 'tap': tap(float(sys.argv[2]), float(sys.argv[3]))
    elif c == 'tapt': print(tapt(sys.argv[2], len(sys.argv) > 3 and sys.argv[3] == 'exact'))
    elif c == 'shot': open(sys.argv[2], 'wb').write(sh('exec-out', 'screencap', '-p', binary=True)); print('saved', sys.argv[2])
    elif c == 'swipe': sh('shell', 'input', 'swipe', *sys.argv[2:6], sys.argv[6] if len(sys.argv) > 6 else '400')
    elif c == 'back': sh('shell', 'input', 'keyevent', 'KEYCODE_BACK')
    elif c == 'launch': sh('shell', 'monkey', '-p', PKG, '-c', 'android.intent.category.LAUNCHER', '1')
