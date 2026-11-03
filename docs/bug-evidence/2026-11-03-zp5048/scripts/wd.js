// Small raw-WebDriver helpers for webview context switching (lane session from ios.js).
module.exports = (s) => {
  const base = 'http://127.0.0.1:4723/session/' + s.sid;
  async function wd(method, url, body) {
    const r = await fetch(base + url, { method, headers: { 'Content-Type': 'application/json' }, body: body === undefined ? undefined : JSON.stringify(body) });
    const j = await r.json().catch(() => ({}));
    if (!r.ok || (j.value && j.value.error)) throw new Error(method + ' ' + url + ' -> ' + JSON.stringify(j).slice(0, 400));
    return j.value;
  }
  async function webCtx() {
    const list = await s.exec('mobile: getContexts', {});
    const w = list.find((c) => /WEBVIEW/.test(c.id) && /index\.html/.test(c.url || ''));
    return w ? w.id : null;
  }
  async function inWeb(script, args = []) {
    const id = await webCtx();
    if (!id) throw new Error('no SLD webview context');
    await wd('POST', '/context', { name: id });
    try { return await wd('POST', '/execute/sync', { script, args }); }
    finally { await wd('POST', '/context', { name: 'NATIVE_APP' }); }
  }
  return { wd, webCtx, inWeb };
};
