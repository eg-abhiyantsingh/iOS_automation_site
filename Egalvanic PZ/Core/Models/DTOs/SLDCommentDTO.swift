import Foundation

struct SLDCommentDTO: Codable {
    let id: UUID
    let body: String
    let created_at: Date
    let modified_at: Date
    let author: UUID
    let author_name: String
    let sld_id: UUID
    let x: Float
    let y: Float
    let width: Int
    let height: Int
    
    enum CodingKeys: String, CodingKey {
        case id
        case body
        case created_at
        case modified_at
        case author
        case author_name
        case sld_id
        case x
        case y
        case width
        case height
    }
}