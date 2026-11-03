import AppKit
// render.swift spec.json outdir  -> outdir/seg-<i>.png, one full-canvas overlay per segment:
// an opaque caption band on top (title, step, result) + transparent area with highlight boxes (point coords of the 402-pt screen).
struct Box: Codable { let x: Double; let y: Double; let w: Double; let h: Double; let label: String?; let color: String? }
struct Seg: Codable { let a: Double; let b: Double; let step: String; let result: String?; let color: String?; let boxes: [Box]? }
struct Spec: Codable { let title: String; let W: Int; let H: Int; let band: Int; let ptW: Double; let ptH: Double; let segments: [Seg] }
let args = CommandLine.arguments
let spec = try! JSONDecoder().decode(Spec.self, from: Data(contentsOf: URL(fileURLWithPath: args[1])))
let outdir = args[2]
let W = spec.W, H = spec.H + spec.band
let sx = Double(spec.W) / spec.ptW, sy = Double(spec.H) / spec.ptH
func col(_ s: String?) -> NSColor { switch s { case "green": return NSColor(calibratedRed: 0.05, green: 0.55, blue: 0.15, alpha: 1); case "red": return NSColor(calibratedRed: 0.86, green: 0.1, blue: 0.1, alpha: 1); default: return NSColor.black } }
for (i, seg) in spec.segments.enumerated() {
  let rep = NSBitmapImageRep(bitmapDataPlanes: nil, pixelsWide: W, pixelsHigh: H, bitsPerSample: 8, samplesPerPixel: 4, hasAlpha: true, isPlanar: false, colorSpaceName: .deviceRGB, bytesPerRow: 0, bitsPerPixel: 0)!
  rep.size = NSSize(width: W, height: H)
  let ctx = NSGraphicsContext(bitmapImageRep: rep)!
  NSGraphicsContext.saveGraphicsState(); NSGraphicsContext.current = ctx
  // flip to top-down coordinates
  let t = NSAffineTransform(); t.translateX(by: 0, yBy: CGFloat(H)); t.scaleX(by: 1, yBy: -1); t.concat()
  NSColor.clear.setFill(); NSRect(x: 0, y: 0, width: W, height: H).fill(using: .copy)
  NSColor.white.setFill(); NSRect(x: 0, y: 0, width: W, height: spec.band).fill()
  NSColor(calibratedWhite: 0.75, alpha: 1).setFill(); NSRect(x: 0, y: spec.band - 2, width: W, height: 2).fill()
  let para = NSMutableParagraphStyle(); para.lineBreakMode = .byWordWrapping
  func draw(_ s: String, _ r: NSRect, _ size: CGFloat, _ c: NSColor, bold: Bool = true) {
    let attr: [NSAttributedString.Key: Any] = [.font: bold ? NSFont.boldSystemFont(ofSize: size) : NSFont.systemFont(ofSize: size), .foregroundColor: c, .paragraphStyle: para]
    let ns = NSAttributedString(string: s, attributes: attr)
    // flipped context: draw text with its own flip so glyphs are upright
    NSGraphicsContext.saveGraphicsState()
    let f = NSAffineTransform(); f.translateX(by: 0, yBy: r.origin.y + r.size.height); f.scaleX(by: 1, yBy: -1); f.concat()
    ns.draw(with: NSRect(x: r.origin.x, y: 0, width: r.size.width, height: r.size.height), options: [.usesLineFragmentOrigin], context: nil)
    NSGraphicsContext.restoreGraphicsState()
  }
  // text rects are given top-down; draw(with:) in the local flipped-back space draws from the rect's bottom, so lay out by measuring
  func drawTop(_ s: String, x: CGFloat, y: CGFloat, w: CGFloat, size: CGFloat, c: NSColor, bold: Bool = true) -> CGFloat {
    let attr: [NSAttributedString.Key: Any] = [.font: bold ? NSFont.boldSystemFont(ofSize: size) : NSFont.systemFont(ofSize: size), .foregroundColor: c, .paragraphStyle: para]
    let ns = NSAttributedString(string: s, attributes: attr)
    let br = ns.boundingRect(with: NSSize(width: w, height: 1000), options: [.usesLineFragmentOrigin])
    NSGraphicsContext.saveGraphicsState()
    let f = NSAffineTransform(); f.translateX(by: x, yBy: y + ceil(br.height)); f.scaleX(by: 1, yBy: -1); f.concat()
    ns.draw(with: NSRect(x: 0, y: 0, width: w, height: ceil(br.height)), options: [.usesLineFragmentOrigin], context: nil)
    NSGraphicsContext.restoreGraphicsState()
    return ceil(br.height)
  }
  var y: CGFloat = 10
  y += drawTop(spec.title, x: 14, y: y, w: CGFloat(W - 28), size: 16, c: NSColor(calibratedWhite: 0.35, alpha: 1)) + 8
  y += drawTop(seg.step, x: 14, y: y, w: CGFloat(W - 28), size: 21, c: .black) + 8
  if let r = seg.result, !r.isEmpty { _ = drawTop(r, x: 14, y: y, w: CGFloat(W - 28), size: 20, c: col(seg.color)) }
  for b in seg.boxes ?? [] {
    let c = col(b.color ?? seg.color)
    let rect = NSRect(x: b.x * sx, y: Double(spec.band) + b.y * sy, width: b.w * sx, height: b.h * sy)
    c.setStroke(); let p = NSBezierPath(roundedRect: rect, xRadius: 8, yRadius: 8); p.lineWidth = 5; p.stroke()
    if let l = b.label, !l.isEmpty {
      let attr: [NSAttributedString.Key: Any] = [.font: NSFont.boldSystemFont(ofSize: 17), .foregroundColor: NSColor.white]
      let sz = (l as NSString).size(withAttributes: attr)
      var lx = rect.origin.x; if lx + sz.width + 12 > CGFloat(W) { lx = CGFloat(W) - sz.width - 14 }
      var ly = rect.origin.y + rect.size.height + 4; if ly + sz.height + 6 > CGFloat(H) { ly = rect.origin.y - sz.height - 10 }
      c.setFill(); NSBezierPath(roundedRect: NSRect(x: lx, y: ly, width: sz.width + 12, height: sz.height + 6), xRadius: 6, yRadius: 6).fill()
      _ = drawTop(l, x: lx + 6, y: ly + 3, w: sz.width + 4, size: 17, c: .white)
    }
  }
  NSGraphicsContext.restoreGraphicsState()
  try! rep.representation(using: .png, properties: [:])!.write(to: URL(fileURLWithPath: "\(outdir)/seg-\(String(format: "%02d", i)).png"))
}
print("rendered \(spec.segments.count) overlays \(W)x\(H)")
