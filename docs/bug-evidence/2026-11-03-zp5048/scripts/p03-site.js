// close the Work Orders sheet, then switch the app's site with its own Sites picker. Stops (no tap) on any alert.
module.exports = async (s, ctx) => {
  const SITE = process.env.SITE || 'QA-DEMO ZP-4800 SLD canvas v2.4 (delete me)';
  const Q = process.env.SITEQ || 'ZP-4800';
  const out = { t: [] };
  const mark = (x) => out.t.push(new Date().toISOString().slice(11, 19) + ' ' + x);
  const done = await s.exists({ pred: 'type == "XCUIElementTypeButton" AND label == "Done"' }, 1500);
  if (done && (await s.texts()).includes('Work Orders')) { await s.tapEl(done); await s.sleep(2000); mark('closed Work Orders sheet'); }
  if (!(await s.exists({ contains: 'Welcome to' }, 1500))) { const st = await s.exists({ label: 'Site' }, 1500); if (st) { await s.tapEl(st); await s.sleep(2000); } }
  out.home0 = (await s.texts()).slice(0, 40);
  await s.shot(ctx.dir + '/p02-home-before.png');
  if (out.home0.some((x) => x.includes('Welcome to ' + SITE))) { out.already = true; return out; }
  await s.tapText('Sites'); await s.sleep(3000); mark('Sites opened');
  for (let i = 0; i < 15; i++) { const t = await s.texts(); if (!t.includes('No sites available') && t.some((x) => /Wild Goose|QA-DEMO/.test(x))) break; await s.sleep(2000); }
  const sf = await s.waitFor({ chain: '**/XCUIElementTypeTextField' }, 8000);
  await s.tapEl(sf); await s.sleep(500);
  await s.type(sf, Q); await s.sleep(2500);
  await s.hideKeyboard(); await s.sleep(800);
  out.rows = (await s.texts()).filter((x) => x.includes(Q)).slice(0, 10);
  const row = await s.waitFor({ pred: `type == "XCUIElementTypeButton" AND (label == "${SITE}" OR label BEGINSWITH "${SITE},")` }, 8000);
  await s.tapEl(row); mark('site row tapped');
  for (let i = 0; i < 90; i++) {
    await s.sleep(1500);
    const al = await s.alertText();
    if (al) { out.alert = al; out.alertButtons = (await s.texts()).slice(0, 20); await s.shot(ctx.dir + '/p02-alert.png'); mark('alert, stopped'); return out; }
    const t = (await s.texts()).join(' | ');
    if (t.includes('Welcome to ' + SITE) || (t.includes(SITE) && /Quick Actions/.test(t))) { mark('home on new site'); break; }
    if (i % 10 === 9) out['screen' + i] = t.slice(0, 400);
  }
  await s.sleep(3000);
  out.home = (await s.texts()).slice(0, 30);
  await s.shot(ctx.dir + '/p02-home-new-site.png');
  return out;
};
