#!/usr/bin/env python3
"""Android 1.39 demo: asset › Forms opens the first form directly (no form list, Add Form hidden at the end of the chip row).
Starts on ZP-5135 Details (active). Logs epoch times; results are written only after reading the screen."""
import sys, time, os
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import ad

LOG = open(os.path.join(os.path.dirname(os.path.abspath(__file__)), 'forms-steps.txt'), 'a')
ASSET = 'QA-DEMO ZP-4874 SEP asset 27 Oct'


def log(msg):
    line = '%.1f  %s' % (time.time(), msg)
    print(line); LOG.write(line + '\n'); LOG.flush()


def must(txt, exact=False):
    n = ad.tapt(txt, exact, tries=8)
    if not n:
        log('NOT FOUND ' + txt); raise SystemExit(1)
    return n


def chips():
    """Form chips in the top row (y < 260) and whether the + (Add Form) is fully on screen."""
    ns = ad.nodes()
    row = [ad.label(n) for n in ns if n['bounds'][1] < 260 and n['bounds'][1] > 120 and ad.label(n)]
    plus = [n for n in ns if ad.label(n) == 'Add Form']
    on = bool(plus) and plus[0]['bounds'][2] <= 1080 and plus[0]['bounds'][0] >= 0
    return row, on


log('START Android 1.39, active work order QA-DEMO ZP-5135')
time.sleep(4)
must('Back', exact=True)
time.sleep(4)
log('STEP1 Work Orders: open "Job - Nov 2, 6:28 PM" (several services)')
must('Job - Nov 2, 6:28 PM')
time.sleep(3)
must('Start Session', exact=True)
time.sleep(8)
log('STEP2 Assets tab › room Test')
must('Assets', exact=True)
time.sleep(4)
must('Test', exact=True)
time.sleep(6)
log('STEP3 tap the asset ' + ASSET)
must(ASSET, exact=True)
time.sleep(5)
labels = [ad.label(n) for n in ad.nodes()]
log('STEP3 result: sheet shows %s' % [l for l in labels if l in ('Infrared', 'Forms', 'Full Asset Page')])
time.sleep(3)
log('STEP4 tap Forms')
must('Forms', exact=True)
time.sleep(4)
row, on = chips()
title = [ad.label(n) for n in ad.nodes() if ad.label(n) == 'Edit Form']
log('STEP4 result: screen %s, chip row %s, Add Form fully on screen: %s' % (title, row, on))
time.sleep(8)
log('STEP5 swipe the chip row to the left')
ad.sh('shell', 'input', 'swipe', '900', '188', '100', '188', '500')
time.sleep(1.5)
ad.sh('shell', 'input', 'swipe', '900', '188', '100', '188', '500')
time.sleep(2)
row, on = chips()
log('STEP5 result: chip row %s, Add Form (+) on screen: %s' % (row, on))
time.sleep(6)
log('STEP6 tap + (Add Form)')
must('Add Form', exact=True)
time.sleep(4)
must('Standalone Form', exact=True)
time.sleep(3)
labels = [ad.label(n) for n in ad.nodes()]
log('STEP6 result: Add Form sheet open, tabs %s' % [l for l in labels if l in ('Procedure', 'Standalone Form')])
time.sleep(5)
log('STEP7 close the Add Form sheet and the form (nothing added or saved)')
must('Close', exact=True)
time.sleep(2.5)
ad.tap(90, 336)
time.sleep(4)
log('STEP8 long-press the asset')
n = ad.find(ASSET, exact=True)
x1, y1, x2, y2 = n['bounds']
ad.sh('shell', 'input', 'swipe', str((x1 + x2) // 2), str((y1 + y2) // 2), str((x1 + x2) // 2 + 2), str((y1 + y2) // 2 + 2), '1300')
time.sleep(3)
labels = [ad.label(n) for n in ad.nodes()]
fm = [l for l in labels if l.startswith('Forms')]
log('STEP8 result: menu has %s' % fm)
time.sleep(5)
log('STEP9 tap %s' % (fm[0] if fm else 'Forms'))
must(fm[0] if fm else 'Forms', exact=True)
time.sleep(4)
row, on = chips()
log('STEP9 result: chip row %s, Add Form on screen: %s' % (row, on))
time.sleep(7)
ad.tap(90, 336)
time.sleep(3)
log('END')
