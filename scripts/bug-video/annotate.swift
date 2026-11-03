import AppKit
// annotate.swift in.(jpg|png) out.png "x,y,w,h,label" ... : red rounded boxes + white-on-red label pills (image pixel coords, top-left origin)
let a = CommandLine.arguments
let img = NSImage(contentsOfFile: a[1])!
let rep0 = img.representations[0]; let W = rep0.pixelsWide, H = rep0.pixelsHigh
let rep = NSBitmapImageRep(bitmapDataPlanes: nil, pixelsWide: W, pixelsHigh: H, bitsPerSample: 8, samplesPerPixel: 4, hasAlpha: true, isPlanar: false, colorSpaceName: .deviceRGB, bytesPerRow: 0, bitsPerPixel: 0)!
rep.size = NSSize(width: W, height: H)
NSGraphicsContext.saveGraphicsState(); NSGraphicsContext.current = NSGraphicsContext(bitmapImageRep: rep)
img.draw(in: NSRect(x: 0, y: 0, width: W, height: H))
let red = NSColor(calibratedRed: 0.86, green: 0.1, blue: 0.1, alpha: 1)
for spec in a.dropFirst(3) {
  let p = spec.split(separator: ",", maxSplits: 4).map(String.init)
  let x = Double(p[0])!, y = Double(p[1])!, w = Double(p[2])!, h = Double(p[3])!; let label = p.count > 4 ? p[4] : ""
  let r = NSRect(x: x, y: Double(H) - y - h, width: w, height: h)
  red.setStroke(); let bp = NSBezierPath(roundedRect: r, xRadius: 6, yRadius: 6); bp.lineWidth = 4; bp.stroke()
  if !label.isEmpty {
    let attr: [NSAttributedString.Key: Any] = [.font: NSFont.boldSystemFont(ofSize: 15), .foregroundColor: NSColor.white]
    let sz = (label as NSString).size(withAttributes: attr)
    var lx = r.minX; if lx + sz.width + 14 > CGFloat(W) { lx = CGFloat(W) - sz.width - 16 }
    var ly = r.minY - sz.height - 10; if ly < 2 { ly = r.maxY + 4 }
    red.setFill(); NSBezierPath(roundedRect: NSRect(x: lx, y: ly, width: sz.width + 12, height: sz.height + 6), xRadius: 5, yRadius: 5).fill()
    (label as NSString).draw(at: NSPoint(x: lx + 6, y: ly + 3), withAttributes: attr)
  }
}
NSGraphicsContext.restoreGraphicsState()
try! rep.representation(using: .png, properties: [:])!.write(to: URL(fileURLWithPath: a[2]))
print("wrote", a[2], W, H)
