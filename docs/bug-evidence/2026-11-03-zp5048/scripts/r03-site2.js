// ZP-5048 on a second site: Android Site 2, view "new 1" (11 assets / 6 connections, 5 with view-specific bends).
// Same flow as site 1: view fresh -> rename asset -> SLD tab -> pick view again -> All Assets > view -> SLD Refresh -> rename back.
module.exports = async (s, ctx) => {
  const EV = '/Users/abhiyantsingh/Downloads/iOS_automation_site/docs/bug-evidence/2026-11-03-zp5048/site2-android-site-2';
  const VIEW = 'new 1', ASSET = 'Panelboard-2', CLS = 'Panelboard', NEWNAME = 'Panelboard-2 ZP-5048';
  const VIEWLABELS = ['MCC-2', 'Circuit Breaker-1 new2', 'Utility-1', 'Relay-145646646', 'Panelboard-11111', 'Fuse-2', 'Relay-2 Panel111111', 'Panelboard-2', 'Panelboard-3', 'Fuse-1', 'MCC-1'];
  const KEYS = ['dd6d39a2', '0a9c429c', 'a6733252', '600eef71', '5e552770', 'afc7c04c'];
  const W = require(ctx.dir + '/wd.js')(s);
  const FIND = `const G = window.go; let d = null; if (G) for (const el of document.querySelectorAll('div')) { try { const x = G.Diagram.fromDiv(el); if (x) { const r = el.getBoundingClientRect(); if (r.width > 10) { d = x; break; } } } catch (e) {} }`;
  const hook = () => W.inWeb(`
    if (!window.__qa) window.__qa = { installedAt: Date.now(), calls: [], logs: [] };
    if (window.updateData && !window.updateData.__qaWrapped) {
      const orig = window.updateData;
      const wrapped = function (lt) {
        try { const data = typeof lt === 'string' ? JSON.parse(lt) : lt; const edges = (data && data.edges) || [];
          window.__qa.calls.push({ t: Date.now(), nodes: (data.nodes || []).length, edges: edges.length, vspTrue: edges.filter((e) => e.has_view_specific_points === true).length }); } catch (e) { window.__qa.calls.push({ t: Date.now(), err: String(e).slice(0, 200) }); }
        return orig.apply(this, arguments);
      };
      wrapped.__qaWrapped = true; window.updateData = wrapped;
    }
    if (!console.log.__qaWrapped) { const ol = console.log; const cl = function () { try { const m = Array.from(arguments).map((a) => typeof a === 'string' ? a : (() => { try { return JSON.stringify(a); } catch (e) { return String(a); } })()).join(' '); if (/window\\.updateData/i.test(m)) window.__qa.logs.push(new Date().toISOString().slice(11, 19) + ' ' + m.slice(0, 200)); } catch (e) {} return ol.apply(this, arguments); }; cl.__qaWrapped = true; console.log = cl; }
    return { installed: true, wrapped: !!(window.updateData && window.updateData.__qaWrapped) };`);
  const readHook = () => W.inWeb(`return window.__qa ? { calls: window.__qa.calls.slice(-6), nCalls: window.__qa.calls.length, logs: window.__qa.logs.slice(-6) } : { gone: true };`).catch((e) => String(e).slice(0, 80));
  const state = () => W.inWeb(`
    ${FIND}
    if (!d) return 'no diagram';
    const VL = ${JSON.stringify(VIEWLABELS)}; const KEYS = ${JSON.stringify(KEYS)};
    const all = []; d.links.each((l) => all.push(l));
    let inView = 0, outView = 0, elbows = 0; const outSample = [];
    d.nodes.each((n) => { if (n.category === 'elbow') { elbows++; return; } const t = String(n.data.text || n.data.label || ''); if (!t) return; if (VL.some((v) => t === v || t.startsWith(v + ' '))) inView++; else { outView++; if (outSample.length < 6) outSample.push(t); } });
    const perEdge = {};
    for (const k of KEYS) { const parts = all.filter((l) => String(l.data.key || '').toLowerCase().startsWith(k) || String(l.data.edgeKey || '').toLowerCase().startsWith(k));
      let el = 0; d.nodes.each((n) => { if (n.category === 'elbow' && String(n.data.edgeKey || '').toLowerCase().startsWith(k)) el++; });
      perEdge[k] = parts.map((l) => l.category || '-').join('/') + ' e' + el + ' pts ' + JSON.stringify(parts.map((l) => { const a = []; for (let i = 0; i < l.pointsCount; i++) { const q = l.getPoint(i); a.push([Math.round(q.x), Math.round(q.y)]); } return a; })); }
    return { nodes: d.nodes.count, links: d.links.count, inView, outView, outSample, elbows, hidden: all.filter((l) => l.category === 'hiddenEdge').length, perEdge };`);
  let RECT = null;
  const frameView = () => W.inWeb(`${FIND} if (!d) return null; const KEYS = ${JSON.stringify(KEYS)}; let R = ${JSON.stringify(RECT)}; let r = R ? new go.Rect(R[0], R[1], R[2], R[3]) : null;
    if (!r) { d.links.each((l) => { const k = String(l.data.key || l.data.edgeKey || '').toLowerCase(); if (KEYS.some((x) => k.startsWith(x))) { const b = l.actualBounds; r = r ? r.unionRect(b) : b.copy(); } }); if (!r) return null; r.inflate(140, 140); }
    d.clearSelection(); d.zoomToRect(r); return [Math.round(r.x), Math.round(r.y), Math.round(r.width), Math.round(r.height), Math.round(d.scale * 1000) / 1000];`).then((v) => { if (v && !RECT) RECT = v.slice(0, 4); return v; }).catch((e) => String(e).slice(0, 80));
  const out = { t: [] };
  const mark = (x) => out.t.push(new Date().toISOString().slice(11, 19) + ' ' + x);
  const dismissKb = async () => { for (let i = 0; i < 3; i++) { const kb = await s.exists({ chain: '**/XCUIElementTypeKeyboard' }, 600); if (!kb) return true; if (i === 0) await s.hideKeyboard(); else { const k = await s.exists({ pred: 'type == "XCUIElementTypeButton" AND (label == "search" OR label == "Search" OR label == "return" OR label == "Return" OR label == "Done")' }, 800); if (k) await s.tapEl(k); else await s.tap(200, 90); } await s.sleep(700); } return true; };
  const pickView = async (name) => {
    await s.tapText('Views'); await s.sleep(2500);
    let target = null;
    for (let i = 0; i < 6; i++) {
      const els = await s.findAll({ pred: `type == "XCUIElementTypeButton" AND label == "${name}"` });
      for (const el of els) { const r = await s.rect(el); if (r.y > 120 && r.y + r.height <= 840) { target = el; break; } }
      if (target) break;
      await s.exec('mobile: dragFromToForDuration', { duration: 0.6, fromX: 200, fromY: 800, toX: 200, toY: 560 }); await s.sleep(1200);
    }
    if (!target) { out.viewsSheet = (await s.texts()).slice(0, 40); throw new Error('row not found in Views sheet: ' + name); }
    await s.tapEl(target); mark('picked ' + name); await s.sleep(10000);
  };
  const headerBack = async () => { const els = await s.findAll({ pred: 'type == "XCUIElementTypeButton" AND label == "All Assets"' }); for (const el of els) { const r = await s.rect(el); if (r.y < 110) { await s.tapEl(el); mark('header back to All Assets'); await s.sleep(10000); return true; } } return false; };
  const rename = async (from, to, shotBeforeSave) => {
    await s.tapText('Assets'); await s.sleep(3000);
    const sf = await s.waitFor({ pred: 'type == "XCUIElementTypeTextField" AND (placeholderValue == "Search assets..." OR value == "Search assets..." OR value BEGINSWITH "Panelboard" OR value BEGINSWITH "QA")' }, 6000);
    await s.tapEl(sf); await s.sleep(500); await s.clear(sf).catch(() => null); await s.type(sf, from); await s.sleep(3000); await dismissKb(); await s.sleep(800);
    const row = await s.waitFor({ pred: `type == "XCUIElementTypeButton" AND (label == "${from}, ${CLS}" OR label BEGINSWITH "${from}, ${CLS},")` }, 8000);
    await s.tapEl(row); await s.sleep(4000);
    const NF = { pred: 'type == "XCUIElementTypeTextField" AND placeholderValue == "Enter asset name"' };
    const nameField = await s.waitFor({ pred: `type == "XCUIElementTypeTextField" AND (value == "${from}" OR placeholderValue == "Enter asset name")` }, 8000);
    out['nameBefore_' + to] = await s.attr(nameField, 'value').catch(() => null);
    await s.tapEl(nameField); await s.sleep(1200);
    await s.clear(await s.waitFor(NF, 5000)); await s.sleep(500);
    await s.type(await s.waitFor(NF, 5000), to); await s.sleep(800);
    const typed = await s.attr(await s.waitFor(NF, 5000), 'value').catch(() => null);
    await dismissKb(); await s.sleep(1000);
    if (shotBeforeSave) await s.shot(shotBeforeSave);
    const save = await s.waitFor({ pred: 'type == "XCUIElementTypeButton" AND label BEGINSWITH "Save"' }, 4000);
    await s.tapEl(save); mark('saved name ' + to); await s.sleep(4000);
    const c2 = await s.exists({ chain: '**/XCUIElementTypeButton[`label == "Close"`]' }, 1500);
    if (c2 && (await s.texts()).includes('Asset Details')) { await s.tapEl(c2); await s.sleep(1500); }
    await dismissKb();
    return { typed, list: (await s.texts()).filter((x) => x.startsWith(ASSET)).slice(0, 6) };
  };
  const snap = async (tag, file) => { const st = await state().catch((e) => String(e).slice(0, 80)); const top = (await s.texts()).slice(0, 12); if (file) await s.shot(EV + '/' + file.replace('.png', '-whole.png')); const fr = await frameView(); await s.sleep(1500); if (file) await s.shot(EV + '/' + file); mark('shot ' + (file || tag)); return { st, fr, top }; };
  try {
    for (const b of ['Cancel', 'Close']) { const cl = await s.exists({ chain: '**/XCUIElementTypeButton[`label == "' + b + '"`]' }, 1000); if (cl && (await s.texts()).includes('Asset Details')) { await s.tapEl(cl); await s.sleep(1500); } }
    await dismissKb();
    await s.tapText('SLD'); await s.sleep(12000);
    if ((await s.texts()).includes('Read Only')) out.backA = await headerBack();
    out.top0 = (await s.texts()).slice(0, 6);
    await pickView(VIEW);
    out.hookA = await hook();
    out.A = await snap('A', '1-ios-view-before-asset-edit.png');
    out.rB = await rename(ASSET, NEWNAME, EV + '/2-ios-asset-name-edit-before-save.png');
    await s.tapText('SLD'); await s.sleep(8000); mark('SLD after edit');
    out.hookC = await readHook();
    out.C = await snap('C', '3-ios-view-after-asset-save.png');
    await pickView(VIEW);
    out.D = await snap('D', '4-ios-same-view-chosen-again.png');
    out.backD2 = await headerBack();
    await pickView(VIEW);
    out.D2 = await snap('D2', '5-ios-view-reopened-from-all-assets.png');
    { const rf = await s.exists({ pred: 'type == "XCUIElementTypeButton" AND (label == "Refresh" OR name == "Refresh")' }, 3000); out.refreshBtn = !!rf; if (rf) { await s.tapEl(rf); mark('SLD Refresh tapped'); await s.sleep(12000); } }
    out.D3 = await snap('D3', '6-ios-view-after-sld-refresh-control.png');
    out.backE = await headerBack();
    out.rE = await rename(NEWNAME, ASSET);
    await s.tapText('SLD'); await s.sleep(8000);
    out.E = await snap('E', null);
  } catch (e) {
    out.err = String(e).slice(0, 400);
    try { out.errScreen = (await s.texts()).slice(0, 40); await s.shot(ctx.dir + '/r03-err.png'); } catch (e2) {}
  }
  return out;
};
