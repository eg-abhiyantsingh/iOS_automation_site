// s23: clean reproduction with evidence (corrected: view loaded fresh first; Views-sheet rows picked by position; header back to All Assets).
// A. View "QA-DEMO ZP-4816 view" freshly selected (correct). Recorder on. Shot 09.
// B. Assets > QA PNL1 > Asset Name "QA PNL1 ZP-4816" (shot 10 before Save) > Save Changes.
// C. SLD tab (view still open): recorder + canvas. Shot 11.
// D. Views > select the same view again: canvas back to normal (control). Shot 12.
// E. Views > All Assets, recorder on. Assets > rename back to "QA PNL1" > Save. SLD tab: All Nodes hides still drawn (control). Shot 13.
module.exports = async (s, ctx) => {
  const EV = '/Users/abhiyantsingh/Downloads/iOS_automation_site/docs/bug-evidence/2026-11-03-zp5048/site1-zp4800';
  const SP = '/private/tmp/claude-501/-Users-abhiyantsingh-Downloads-iOS-automation-site/7280aec9-da63-470e-a865-bea0b4208495/scratchpad/zp5048';
  const X = require(ctx.dir + '/ilib.js')(s, ctx);
  const fmt = (l) => l.map((e) => `${e.type}|${e.text}|${e.x},${e.y},${e.w}x${e.h}`);
  const out = { t: [] };
  const mark = (x) => out.t.push(new Date().toISOString().slice(11, 19) + ' ' + x);
  const short = (st) => (st && st.edges) ? { nodes: st.nodes, links: st.links, hidden: st.hiddenEdges, ffd: st.edges.ffd9732c.cats.join('/') + ' ' + JSON.stringify(st.edges.ffd9732c.elbows), e771: st.edges['771f40c2'].cats.join('/'), f22: st.edges.f22f1ed4.cats.join('/'), e370: st.edges['370cd90f'].cats.join('/'), e30f: st.edges['30fcae76'].cats.join('/') } : st;
  const labels = () => X.W.inWeb(`const G = window.go; let d = null; for (const el of document.querySelectorAll('div')) { try { const x = G.Diagram.fromDiv(el); if (x) { const r = el.getBoundingClientRect(); if (r.width > 10) { d = x; break; } } } catch (e) {} } if (!d) return null; const r = []; d.nodes.each((n) => { const t = String(n.data.text || n.data.label || ''); if (/^QA PNL1|ORPH/.test(t)) r.push(t); }); return r;`).catch((e) => 'err ' + String(e).slice(0, 60));
  const dismissKb = async () => {
    for (let i = 0; i < 3; i++) {
      const kb = await s.exists({ chain: '**/XCUIElementTypeKeyboard' }, 600);
      if (!kb) return true;
      if (i === 0) await s.hideKeyboard();
      else { const k = await s.exists({ pred: 'type == "XCUIElementTypeButton" AND (label == "search" OR label == "Search" OR label == "return" OR label == "Return" OR label == "Done")' }, 800); if (k) await s.tapEl(k); else await s.tap(200, 90); }
      await s.sleep(700);
    }
    return !(await s.exists({ chain: '**/XCUIElementTypeKeyboard' }, 500));
  };
  const pickView = async (name) => {
    await s.tapText('Views'); await s.sleep(2500);
    let target = null;
    for (let i = 0; i < 4; i++) {
      const els = await s.findAll({ pred: `type == "XCUIElementTypeButton" AND label == "${name}"` });
      for (const el of els) { const r = await s.rect(el); if (r.y > 120 && r.y + r.height <= 840) { target = el; break; } }
      if (target) break;
      await s.exec('mobile: dragFromToForDuration', { duration: 0.6, fromX: 200, fromY: 800, toX: 200, toY: 560 }); await s.sleep(1200);
    }
    if (!target) throw new Error('row not found in Views sheet: ' + name);
    await s.tapEl(target); mark('picked ' + name); await s.sleep(9000);
  };
  const headerBack = async () => {
    const els = await s.findAll({ pred: 'type == "XCUIElementTypeButton" AND label == "All Assets"' });
    for (const el of els) { const r = await s.rect(el); if (r.y < 110) { await s.tapEl(el); mark('header back to All Assets'); await s.sleep(8000); return true; } }
    return false;
  };
  const rename = async (from, to, shotBeforeSave) => {
    await s.tapText('Assets'); await s.sleep(3000);
    const sf = await s.waitFor({ pred: '(type == "XCUIElementTypeTextField") AND (placeholderValue == "Search assets..." OR value == "Search assets..." OR value BEGINSWITH "QA PNL1" OR value == "QA PNL1 q")' }, 6000);
    await s.tapEl(sf); await s.sleep(500); await s.clear(sf).catch(() => null); await s.type(sf, from); await s.sleep(2500); await dismissKb(); await s.sleep(800);
    const row = await s.waitFor({ pred: `type == "XCUIElementTypeButton" AND label == "${from}, Panelboard"` }, 6000);
    await s.tapEl(row); await s.sleep(4000);
    const nameField = await s.waitFor({ pred: `type == "XCUIElementTypeTextField" AND (value == "${from}" OR placeholderValue == "Enter asset name")` }, 6000);
    await s.tapEl(nameField); await s.sleep(1200);
    const NF = { pred: 'type == "XCUIElementTypeTextField" AND placeholderValue == "Enter asset name"' };
    await s.clear(await s.waitFor(NF, 5000)); await s.sleep(500);
    await s.type(await s.waitFor(NF, 5000), to); await s.sleep(800);
    const typed = await s.attr(await s.waitFor(NF, 5000), 'value').catch(() => null);
    await dismissKb(); await s.sleep(1000);
    if (shotBeforeSave) await s.shot(shotBeforeSave);
    const save = await s.waitFor({ pred: 'type == "XCUIElementTypeButton" AND label BEGINSWITH "Save"' }, 4000);
    await s.tapEl(save); mark('saved name ' + to); await s.sleep(3500);
    const c2 = await s.exists({ chain: '**/XCUIElementTypeButton[`label == "Close"`]' }, 1500);
    if (c2 && (await s.texts()).includes('Asset Details')) { await s.tapEl(c2); await s.sleep(1500); }
    await dismissKb();
    return { typed, list: (await s.texts()).filter((x) => /PNL1/.test(x)).slice(0, 6) };
  };
  try {
    for (const b of ['Cancel', 'Close']) { const cl = await s.exists({ chain: '**/XCUIElementTypeButton[`label == "' + b + '"`]' }, 1000); if (cl && (await s.texts()).includes('Asset Details')) { await s.tapEl(cl); await s.sleep(1500); } }
    await dismissKb();
    // A: make sure the view is loaded fresh (go to All Assets first, then open the view)
    await s.tapText('SLD'); await s.sleep(5000);
    if ((await s.texts()).includes('Read Only')) out.backA = await headerBack();
    out.top0 = (await s.texts()).slice(0, 4);
    await pickView('QA-DEMO ZP-4816 view');
    out.topA = (await s.texts()).slice(0, 4);
    out.hookA = await X.hook();
    out.sA = short(await X.state()); out.labA = await labels();
    await X.zoom(-230, 30, 680, 980); await s.sleep(1500);
    await s.shot(EV + '/1-ios-view-before-asset-edit-bends-and-hide.png'); mark('shot 09');
    // B
    out.rB = await rename('QA PNL1 q', 'QA PNL1 q ZP-5048', EV + '/2-ios-asset-name-edit-before-save.png');
    // C
    await s.tapText('SLD'); await s.sleep(6000); mark('SLD after edit');
    out.topC = (await s.texts()).slice(0, 14);
    out.hookC = await X.readHook(); out.sC = short(await X.state()); out.labC = await labels();
    await X.zoom(-230, 30, 680, 980); await s.sleep(1500);
    await s.shot(EV + '/3-ios-view-after-asset-save.png'); mark('shot 11');
    // D: choose the same view again in the Views sheet
    await pickView('QA-DEMO ZP-4816 view');
    out.hookD = await X.readHook().catch((e) => String(e).slice(0, 80));
    out.sD = short(await X.state()); out.labD = await labels();
    await X.zoom(-230, 30, 680, 980); await s.sleep(1500);
    await s.shot(EV + '/4-ios-same-view-chosen-again.png'); mark('shot 12');
    // D2: header back to All Assets, then open the view again
    out.backD2 = await headerBack();
    await pickView('QA-DEMO ZP-4816 view');
    out.sD2 = short(await X.state()); out.labD2 = await labels();
    await X.zoom(-230, 30, 680, 980); await s.sleep(1500);
    await s.shot(EV + '/5-ios-view-reopened-from-all-assets.png'); mark('shot 13');
    // D3: control, the SLD Refresh button on the view
    { const rf = await s.exists({ pred: 'type == "XCUIElementTypeButton" AND (label == "Refresh" OR name == "Refresh")' }, 3000); out.refreshBtn = !!rf; if (rf) { const n0 = (await X.readHook().catch(() => ({}))).nCalls; await s.tapEl(rf); mark('SLD Refresh tapped'); await s.sleep(10000); out.hookD3 = await X.readHook().catch((e) => String(e).slice(0, 80)); out.hookD3n0 = n0; } }
    out.sD3 = short(await X.state()); out.labD3 = await labels(); out.topD3 = (await s.texts()).slice(0, 10);
    await X.zoom(-230, 30, 680, 980); await s.sleep(1500);
    await s.shot(EV + '/6-ios-view-after-sld-refresh-control.png'); mark('shot 6');
    // E: control in All Assets
    out.backE = await headerBack();
    out.topE0 = (await s.texts()).slice(0, 4);
    out.hookE = await X.hook();
    out.sE0 = short(await X.state());
    out.rE = await rename('QA PNL1 q ZP-5048', 'QA PNL1 q');
    await s.tapText('SLD'); await s.sleep(6000); mark('SLD (All Assets) after edit back');
    out.topE = (await s.texts()).slice(0, 4);
    out.hookE2 = await X.readHook(); out.sE = short(await X.state()); out.labE = await labels();
    await X.zoom(-230, 30, 680, 980); await s.sleep(1500);
    await s.shot(EV + '/7-ios-all-assets-after-rename-back.png'); mark('shot 14');
  } catch (e) {
    out.err = String(e).slice(0, 400);
    try { out.errScreen = (await s.texts()).slice(0, 30); await s.shot(SP + '/s22-err.png'); } catch (e2) {}
  }
  return out;
};
