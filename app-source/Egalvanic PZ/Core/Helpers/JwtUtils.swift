import Foundation

enum JwtUtils {
    /// Returns the `exp` claim as Unix epoch seconds (RFC 7519), or nil if undecodable.
    static func decodeJwtExp(_ token: String) -> TimeInterval? {
        guard let payload = decodeJwtPayload(token),
              let exp = payload["exp"] as? Double else { return nil }
        return exp
    }

    /// Returns the JWT payload as a dictionary. No signature verification.
    static func decodeJwtPayload(_ token: String) -> [String: Any]? {
        let parts = token.split(separator: ".")
        guard parts.count >= 2 else { return nil }
        var b64 = String(parts[1])
            .replacingOccurrences(of: "-", with: "+")
            .replacingOccurrences(of: "_", with: "/")
        let pad = b64.count % 4
        if pad > 0 { b64 += String(repeating: "=", count: 4 - pad) }
        guard let data = Data(base64Encoded: b64),
              let json = try? JSONSerialization.jsonObject(with: data) as? [String: Any]
        else { return nil }
        return json
    }
}
