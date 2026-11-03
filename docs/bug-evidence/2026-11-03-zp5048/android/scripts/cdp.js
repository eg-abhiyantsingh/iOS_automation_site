// cdp.js: evaluate JS in the Android app's SLD WebView pages over the DevTools protocol (adb forward tcp:9333).
// usage: node cdp.js '<js expression>'  -> prints [{url, id, value}] for each page
const expr = process.argv[2];
(async () => {
  const pages = await (await fetch('http://127.0.0.1:9333/json')).json();
  const out = [];
  for (const p of pages.filter((x) => x.type === 'page')) {
    const ws = new WebSocket(p.webSocketDebuggerUrl);
    await new Promise((r, j) => { ws.onopen = r; ws.onerror = j; });
    const res = await new Promise((resolve) => {
      ws.onmessage = (m) => { const d = JSON.parse(m.data); if (d.id === 1) resolve(d.result); };
      ws.send(JSON.stringify({ id: 1, method: 'Runtime.evaluate', params: { expression: expr, returnByValue: true, awaitPromise: true } }));
      setTimeout(() => resolve({ timeout: true }), 15000);
    });
    ws.close();
    out.push({ id: p.id.slice(0, 6), value: res.result ? res.result.value : res });
  }
  console.log(JSON.stringify(out));
})();
