// ZP-5048 on Android (recorded): view open -> edit asset name from inside the view -> Save -> read the view (CDP) -> rename back.
const fs = require('fs');
const { execFileSync } = require('child_process');
const A = require('./and.js');
const DIR = __dirname, P = DIR + '/demo-progress.txt';
const SITE_VIEW = process.env.VIEW || 'QA-DEMO ZP-4816 view', ASSET = process.env.ASSET || 'QA PNL1 q', NEWNAME = ASSET + ' ZP-5048';
const ZOOM = process.env.ZOOM || '-230,30,680,980';
const SHOT = process.env.SHOTP || 'ev';
const ist = () => new Date(Date.now() + 330 * 60000).toISOString().slice(11, 19);
const say = (m) => fs.appendFileSync(P, ist() + ' IST  ' + m + '\n');
const cdp = (expr) => JSON.parse(execFileSync('node', [DIR + '/cdp.js', expr], { maxBuffer: 1 << 24 }).toString()).find((x) => x.value && x.value.vis === 'visible');
const STATE = fs.readFileSync(DIR + '/js-state.js', 'utf8');
const zoomAndFind = (name) => cdp(`(() => { if (document.visibilityState !== 'visible') return { vis: 'hidden' }; const G = window.go; let d = null, div = null; for (const el of document.querySelectorAll('div')) { try { const x = G.Diagram.fromDiv(el); if (x) { const r = el.getBoundingClientRect(); if (r.width > 10) { d = x; div = el; break; } } } catch (e) {} } if (!d) return { vis: 'visible', nodiagram: true }; ${ZOOM === 'none' ? '' : ZOOM === 'node' ? `d.clearSelection(); d.nodes.each((n) => { if (String(n.data.text || n.data.label) === ${JSON.stringify(name)}) { const b = n.actualBounds.copy(); b.inflate(700, 900); d.zoomToRect(b); } });` : `d.clearSelection(); d.zoomToRect(new go.Rect(${ZOOM}));`} const r = div.getBoundingClientRect(); let p = null; d.nodes.each((n) => { if (String(n.data.text || n.data.label) === ${JSON.stringify(name)}) { const c = d.transformDocToView(n.actualBounds.center); p = [c.x + r.left, c.y + r.top]; } }); return { vis: 'visible', p, w: innerWidth }; })()`);
(async () => {
  const s = await A.open();
  const out = {};
  const pause = (sec) => s.sleep(sec * 1000);
  const wv = async () => { const src = await s.source(); const m = src.match(/<android\.webkit\.WebView[^>]*bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"/); return m ? m.slice(1).map(Number) : null; };
  const toPx = async (p) => { const b = await wv(); const k = (b[2] - b[0]) / 412; return [Math.round(b[0] + p[0] * k), Math.round(b[1] + p[1] * k)]; };
  const badge = async () => { const l = await s.labels(); const i = l.findIndex((e) => e.text === 'Platform Platform'); return i >= 0 && l[i + 1] ? l[i + 1].text : '?'; };
  const summary = (st) => st && st.value ? `${st.value.nodes} assets / ${st.value.links} connections drawn` + (process.env.SITE2 ? '' : `, hidden connections: ${st.value.hidden.length}, bends on QA SWBD1 > QA CB2: ${/e2/.test(st.value.edges.ffd9732c) ? 'yes' : 'no'}`) : 'n/a';
  const pickView = async () => { await s.click(await s.waitFor({ desc: 'Views' }, 8000)); await pause(3); await s.click(await s.waitFor({ text: SITE_VIEW }, 8000)); await pause(9); };
  const rename = async (from, to, slow) => {
    const f = zoomAndFind(from); if (!f || !f.value.p) throw new Error('asset not on canvas: ' + from);
    const [x, y] = await toPx(f.value.p); await pause(slow ? 2 : 1);
    await s.longPress(x, y, 1200); await pause(2);
    await s.click(await s.waitFor({ text: 'Edit' }, 6000)); await pause(slow ? 4 : 3);
    const field = await s.waitFor({ xpath: `//android.widget.EditText[@text=${JSON.stringify(from)}]` }, 8000);
    await s.click(field); await s.sleep(600); await s.clear(field); await s.type(field, to); await pause(slow ? 3 : 1);
    let save = await s.exists({ ui: 'new UiSelector().textMatches("(?i)save( changes)?")' }, 2500);
    if (!save && await s.isKeyboardShown()) { A.adb('shell', 'input', 'keyevent', '4'); await pause(1.5); save = await s.exists({ ui: 'new UiSelector().textMatches("(?i)save( changes)?")' }, 4000); }
    if (!save) { const l = await s.labels(); throw new Error('no Save button; screen: ' + l.map((e) => e.text).slice(0, 30).join(' | ')); }
    await s.click(save); await pause(5);
    const t = (await s.texts()).join(' | '); return t.slice(0, 200);
  };
  try {
    const t0 = (await s.texts()).join(' | ');
    if (t0.includes(SITE_VIEW)) { await s.click(await s.waitFor({ desc: 'Back' }, 5000)); await pause(5); }
    say('Step 1: SLD tab > Views > "' + SITE_VIEW + '"'); await pickView();
    cdp(STATE); zoomAndFind(ASSET); await pause(2);
    out.before = cdp(STATE); out.badgeBefore = await badge();
    say('Step 1 result (CORRECT): ' + summary(out.before) + ', badge ' + out.badgeBefore); await s.shot(DIR + '/' + SHOT + '-1-view-before.png'); await pause(7);
    say('Step 2: long-press ' + ASSET + ' > Edit > Asset Name "' + NEWNAME + '" > Save Changes'); out.r1 = await rename(ASSET, NEWNAME, true);
    say('Step 2: saved, back on: ' + out.r1.slice(0, 60));
    await pause(4); zoomAndFind(NEWNAME); await pause(2);
    out.after = cdp(STATE); out.badgeAfter = await badge();
    say('Step 3 result (' + (out.after && out.before && out.after.value.nodes > out.before.value.nodes + 5 ? 'BUG' : 'NO BUG') + '): ' + summary(out.after) + ', badge ' + out.badgeAfter + '. updateData calls: ' + JSON.stringify(out.after && out.after.value.calls)); await s.shot(DIR + '/' + SHOT + '-3-view-after-save.png'); await pause(9);
    say('Step 4: SLD Refresh (control)'); await s.click(await s.waitFor({ desc: 'Refresh' }, 5000)); await pause(9); zoomAndFind(NEWNAME); await pause(2);
    out.refresh = cdp(STATE); out.badgeRefresh = await badge();
    say('Step 4 result: ' + summary(out.refresh) + ', badge ' + out.badgeRefresh); await s.shot(DIR + '/' + SHOT + '-4-after-refresh.png'); await pause(6);
    say('Cleanup: rename back to "' + ASSET + '"'); out.r2 = await rename(NEWNAME, ASSET, false); await pause(3);
    out.final = cdp(STATE);
    say('Cleanup done: ' + summary(out.final));
    say('DONE');
  } catch (e) { out.err = String(e.message || e).slice(0, 400); say('ERROR: ' + out.err); try { await s.shot(DIR + '/demo-err.png'); } catch (e2) {} }
  console.log('RESULT ' + JSON.stringify(out));
})();
