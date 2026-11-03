(() => {
  const KEYS = ['ffd9732c', '771f40c2', 'f22f1ed4', '370cd90f', '30fcae76'];
  if (!window.__qa) window.__qa = { installedAt: Date.now(), calls: [] };
  if (window.updateData && !window.updateData.__qaWrapped) {
    const orig = window.updateData;
    const w = function (lt) { try { const d = typeof lt === 'string' ? JSON.parse(lt) : lt; const e = (d && d.edges) || []; window.__qa.calls.push({ t: Date.now(), nodes: (d.nodes || []).length, edges: e.length, vsp: e.filter((x) => x.has_view_specific_points === true).length }); } catch (err) { window.__qa.calls.push({ err: String(err).slice(0, 100) }); } return orig.apply(this, arguments); };
    w.__qaWrapped = true; window.updateData = w;
  }
  const G = window.go; let d = null; if (G) for (const el of document.querySelectorAll('div')) { try { const x = G.Diagram.fromDiv(el); if (x) { const r = el.getBoundingClientRect(); if (r.width > 10) { d = x; break; } } } catch (e) {} }
  if (!d) return { vis: document.visibilityState, nodiagram: true };
  const all = []; d.links.each((l) => all.push(l));
  const lab = []; d.nodes.each((n) => { const t = String(n.data.text || n.data.label || ''); if (/PNL1|ORPH/.test(t)) lab.push(t); });
  const e = {}; for (const k of KEYS) { const parts = all.filter((l) => String(l.data.key || '').toLowerCase().startsWith(k) || String(l.data.edgeKey || '').toLowerCase().startsWith(k)); let el = 0; d.nodes.each((n) => { if (n.category === 'elbow' && String(n.data.edgeKey || '').toLowerCase().startsWith(k)) el++; }); e[k] = parts.map((l) => l.category || '-').join('/') + ' e' + el; }
  return { vis: document.visibilityState, nodes: d.nodes.count, links: d.links.count, hidden: all.filter((l) => l.category === 'hiddenEdge').map((l) => String(l.data.key).slice(0, 8)), labels: lab, edges: e, hooked: !!(window.updateData && window.updateData.__qaWrapped), calls: window.__qa.calls.slice(-3) };
})()
