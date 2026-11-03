// iOS-side helpers for ZP-4816: run small read-only snippets in the SLD viewer web view (OTA bundle, GoJS).
// The only page change is a pass-through recorder: it wraps window.updateData and console.log to record each call
// (edge ids, has_view_specific_points, edge_manips) and then calls the original unchanged.
module.exports = (s, ctx) => {
  const W = require(ctx.dir + '/wd.js')(s);
  const FIND = `const G = window.go; let d = null; if (G) for (const el of document.querySelectorAll('div')) { try { const x = G.Diagram.fromDiv(el); if (x) { const r = el.getBoundingClientRect(); if (r.width > 10) { d = x; break; } } } catch (e) {} }`;
  const KEYS = ['ffd9732c', '771f40c2', 'f22f1ed4', '17478b09', '370cd90f', '30fcae76', '627fc9d0', '965ece41'];
  const hook = () => W.inWeb(`
    const KEYS = ${JSON.stringify(KEYS)};
    if (!window.__qa) window.__qa = { installedAt: Date.now(), calls: [], logs: [] };
    if (window.updateData && !window.updateData.__qaWrapped) {
      const orig = window.updateData;
      const wrapped = function (lt) {
        try {
          const data = typeof lt === 'string' ? JSON.parse(lt) : lt;
          const edges = (data && data.edges) || [];
          const pick = edges.filter((e) => KEYS.some((k) => String(e.id || '').toLowerCase().startsWith(k)));
          window.__qa.calls.push({ t: Date.now(), nodes: (data.nodes || []).length, edges: edges.length, topKeys: Object.keys(data || {}).slice(0, 20),
            edgeKeys: edges[0] ? Object.keys(edges[0]).slice(0, 40) : [], vspTrue: edges.filter((e) => e.has_view_specific_points === true).length,
            withManips: edges.filter((e) => e.edge_manips).map((e) => String(e.id).slice(0, 8).toLowerCase()),
            pick: pick.map((e) => ({ id: String(e.id).slice(0, 8).toLowerCase(), vsp: e.has_view_specific_points, em: e.edge_manips === undefined ? 'undefined' : e.edge_manips })) });
        } catch (e) { window.__qa.calls.push({ t: Date.now(), err: String(e).slice(0, 200) }); }
        try { localStorage.setItem('__qaZP4816', JSON.stringify({ savedAt: Date.now(), calls: window.__qa.calls.slice(-4) })); } catch (e) {}
        return orig.apply(this, arguments);
      };
      wrapped.__qaWrapped = true;
      window.updateData = wrapped;
    }
    for (const fn of ['updateNode', 'clearGraph']) {
      if (window[fn] && !window[fn].__qaWrapped) {
        const o = window[fn];
        const w = function (arg) {
          try { let id = null; try { const a = typeof arg === 'string' ? JSON.parse(arg) : arg; id = a && (a.id || a.label) ? String(a.id).slice(0, 8) + ' ' + String(a.label || '') : null; } catch (e) {} (window.__qa.other = window.__qa.other || []).push({ t: Date.now(), fn, id }); } catch (e) {}
          return o.apply(this, arguments);
        };
        w.__qaWrapped = true; window[fn] = w;
      }
    }
    if (!console.log.__qaWrapped) {
      const ol = console.log;
      const cl = function () { try { const m = Array.from(arguments).map((a) => typeof a === 'string' ? a : (() => { try { return JSON.stringify(a); } catch (e) { return String(a); } })()).join(' '); if (/window\\.updateData|clearGraph|updateNode|view/i.test(m)) window.__qa.logs.push(new Date().toISOString().slice(11, 19) + ' ' + m.slice(0, 260)); } catch (e) {} return ol.apply(this, arguments); };
      cl.__qaWrapped = true; console.log = cl;
    }
    return { installed: true, calls: window.__qa.calls.length, wrapped: !!(window.updateData && window.updateData.__qaWrapped) };
  `);
  const readHook = () => W.inWeb(`return window.__qa ? { installedAt: window.__qa.installedAt, wrappedNow: !!(window.updateData && window.updateData.__qaWrapped), calls: window.__qa.calls.slice(-8), nCalls: window.__qa.calls.length, other: (window.__qa.other || []).slice(-8), logs: window.__qa.logs.slice(-14) } : { gone: true, href: location.href };`);
  const state = (keys = KEYS) => W.inWeb(`
    ${FIND}
    if (!d) return 'no diagram';
    const lab = (k) => { const n = d.findNodeForKey(k); return n ? (n.data.text || n.data.label) : String(k).slice(0, 8); };
    const all = []; d.links.each((l) => all.push(l));
    const res = { nodes: d.nodes.count, links: d.links.count, scale: Math.round(d.scale * 1000) / 1000, readOnly: d.isReadOnly, allowMove: d.allowMove, hiddenEdges: all.filter((l) => l.category === 'hiddenEdge').map((l) => String(l.data.key).slice(0, 8).toLowerCase()), edges: {} };
    for (const k of ${JSON.stringify(keys)}) {
      const parts = all.filter((l) => String(l.data.key || '').toLowerCase().startsWith(k) || String(l.data.edgeKey || '').toLowerCase().startsWith(k));
      const elb = []; d.nodes.each((n) => { if (n.category === 'elbow' && String(n.data.edgeKey || '').toLowerCase().startsWith(k)) elb.push([Math.round(n.location.x), Math.round(n.location.y)]); });
      res.edges[k] = { n: parts.length, cats: parts.map((l) => l.category || '(default)'), from: parts[0] ? lab(parts[0].data.from) : null, to: parts.length ? lab(parts[parts.length - 1].data.to) : null,
        hidden: parts.some((l) => !!l.data.hidden), elbows: elb,
        pts: parts.map((l) => { const a = []; for (let i = 0; i < l.pointsCount; i++) { const q = l.getPoint(i); a.push([Math.round(q.x), Math.round(q.y)]); } return a; }) };
    }
    return res;
  `);
  const zoom = (x, y, w, h) => W.inWeb(`${FIND} if (!d) return null; d.clearSelection(); d.zoomToRect(new go.Rect(${x}, ${y}, ${w}, ${h})); return Math.round(d.scale * 1000) / 1000;`);
  return { W, KEYS, hook, readHook, state, zoom };
};
