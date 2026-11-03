// ZP-5048 live demo on the ticket's site, slowed down. Writes each step to demo-progress.txt (IST) as it happens.
const fs = require('fs');
module.exports = async (s, ctx) => {
  const P = ctx.dir + '/demo-progress.txt';
  const X = require(ctx.dir + '/ilib.js')(s, ctx);
  const VIEW = 'QA-DEMO ZP-4816 view', ASSET = 'QA PNL1 q', NEW = 'QA PNL1 q ZP-5048';
  const ist = () => new Date(Date.now() + 330 * 60000).toISOString().slice(11, 19);
  const say = (m) => fs.appendFileSync(P, ist() + ' IST  ' + m + '\n');
  const pause = (sec) => s.sleep(sec * 1000);
  const badge = async () => { const t = await s.texts(); const i = t.indexOf('Vertical scroll bar, 2 pages'); return i >= 0 ? t[i + 1] : '?'; };
  const counts = async () => { const st = await X.state().catch(() => null); return st && st.edges ? `${st.nodes} assets / ${st.links} connections drawn, hidden connections: ${st.hiddenEdges.length}, bends on QA SWBD1 > QA CB2: ${st.edges.ffd9732c.elbows.length ? 'yes' : 'no'}` : 'n/a'; };
  const dismissKb = async () => { for (let i = 0; i < 3; i++) { const kb = await s.exists({ chain: '**/XCUIElementTypeKeyboard' }, 600); if (!kb) return; if (i === 0) await s.hideKeyboard(); else { const k = await s.exists({ pred: 'type == "XCUIElementTypeButton" AND (label == "search" OR label == "Search" OR label == "return" OR label == "Return" OR label == "Done")' }, 800); if (k) await s.tapEl(k); else await s.tap(200, 90); } await s.sleep(700); } };
  const pickView = async (name) => {
    await s.tapText('Views'); await pause(3);
    let target = null;
    for (let i = 0; i < 4; i++) { const els = await s.findAll({ pred: `type == "XCUIElementTypeButton" AND label == "${name}"` }); for (const el of els) { const r = await s.rect(el); if (r.y > 120 && r.y + r.height <= 840) { target = el; break; } } if (target) break; await s.exec('mobile: dragFromToForDuration', { duration: 0.6, fromX: 200, fromY: 800, toX: 200, toY: 560 }); await s.sleep(1200); }
    if (!target) throw new Error('row not found in Views sheet: ' + name);
    await s.tapEl(target); await pause(9);
  };
  const headerBack = async () => { const els = await s.findAll({ pred: 'type == "XCUIElementTypeButton" AND label == "All Assets"' }); for (const el of els) { const r = await s.rect(el); if (r.y < 110) { await s.tapEl(el); await pause(8); return true; } } return false; };
  const rename = async (from, to, slow) => {
    await s.tapText('Assets'); await pause(slow ? 3 : 2);
    const sf = await s.waitFor({ pred: '(type == "XCUIElementTypeTextField") AND (placeholderValue == "Search assets..." OR value == "Search assets..." OR value BEGINSWITH "QA PNL1")' }, 6000);
    await s.tapEl(sf); await s.sleep(500); await s.clear(sf).catch(() => null); await s.type(sf, from); await pause(3); await dismissKb(); await s.sleep(800);
    const row = await s.waitFor({ pred: `type == "XCUIElementTypeButton" AND label == "${from}, Panelboard"` }, 8000);
    await s.tapEl(row); await pause(slow ? 4 : 3);
    const NF = { pred: 'type == "XCUIElementTypeTextField" AND placeholderValue == "Enter asset name"' };
    const nameField = await s.waitFor({ pred: `type == "XCUIElementTypeTextField" AND (value == "${from}" OR placeholderValue == "Enter asset name")` }, 8000);
    await s.tapEl(nameField); await s.sleep(1200);
    await s.clear(await s.waitFor(NF, 5000)); await s.sleep(500);
    await s.type(await s.waitFor(NF, 5000), to); await s.sleep(800);
    await dismissKb(); await pause(slow ? 4 : 1);
    const save = await s.waitFor({ pred: 'type == "XCUIElementTypeButton" AND label BEGINSWITH "Save"' }, 4000);
    await s.tapEl(save); await pause(4);
    const c2 = await s.exists({ chain: '**/XCUIElementTypeButton[`label == "Close"`]' }, 1500);
    if (c2 && (await s.texts()).includes('Asset Details')) { await s.tapEl(c2); await s.sleep(1500); }
    await dismissKb();
  };
  const out = {};
  try {
    for (const b of ['Cancel', 'Close']) { const cl = await s.exists({ chain: '**/XCUIElementTypeButton[`label == "' + b + '"`]' }, 1000); if (cl && (await s.texts()).includes('Asset Details')) { await s.tapEl(cl); await s.sleep(1500); } }
    say('Step 1: SLD tab'); await s.tapText('SLD'); await pause(6);
    if ((await s.texts()).includes('Read Only')) await headerBack();
    say('Step 1: Views > "' + VIEW + '"'); await pickView(VIEW);
    await X.hook(); await X.zoom(-230, 30, 680, 980); await pause(2);
    out.before = await counts(); out.badgeBefore = await badge();
    say('Step 1 result (CORRECT): ' + out.before + ', badge ' + out.badgeBefore); await s.shot(ctx.dir + '/demo-1.png'); await pause(6);
    say('Step 2: Assets > ' + ASSET + ' > Asset Name "' + NEW + '" > Save Changes'); await rename(ASSET, NEW, true);
    say('Step 2: saved');
    say('Step 3: back to the SLD tab (the view is still open)'); await s.tapText('SLD'); await pause(6);
    await X.zoom(-230, 30, 680, 980); await pause(2);
    const h = await X.readHook().catch(() => null); const c = h && h.calls && h.calls[h.calls.length - 1];
    out.after = await counts(); out.badgeAfter = await badge(); out.updateData = c ? `${c.nodes} nodes / ${c.edges} edges, view-specific points on ${c.vspTrue}` : 'n/a';
    say('Step 3 result (BUG): ' + out.after + ', badge ' + out.badgeAfter + '. The app sent the view: ' + out.updateData); await s.shot(ctx.dir + '/demo-3.png'); await pause(8);
    say('Step 4: Views > pick the same view again'); await pickView(VIEW); await X.zoom(-230, 30, 680, 980); await pause(2);
    out.again = await counts(); out.badgeAgain = await badge();
    say('Step 4 result (still BUG): ' + out.again + ', badge ' + out.badgeAgain); await pause(6);
    say('Step 5: All Assets, then the view again'); await headerBack(); await pickView(VIEW); await X.zoom(-230, 30, 680, 980); await pause(2);
    out.reopen = await counts(); out.badgeReopen = await badge();
    say('Step 5 result (correct again): ' + out.reopen + ', badge ' + out.badgeReopen); await pause(6);
    say('Cleanup: All Assets, name back to "' + ASSET + '"'); await headerBack(); await rename(NEW, ASSET, false);
    await s.tapText('SLD'); await pause(4);
    say('DONE');
  } catch (e) { out.err = String(e).slice(0, 300); say('ERROR: ' + out.err); try { await s.shot(ctx.dir + '/demo-err.png'); out.errScreen = (await s.texts()).slice(0, 30); } catch (e2) {} }
  return out;
};
