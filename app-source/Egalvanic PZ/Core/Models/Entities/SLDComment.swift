import Foundation
import SwiftData

@Model
final class SLDComment {
    @Attribute(.unique) var id: UUID
    var body: String
    var createdAt: Date
    var modifiedAt: Date
    var author: UUID
    var authorName: String
    var sldId: UUID
    var x: Float
    var y: Float
    var width: Int
    var height: Int
    
    var sld: SLDV2?
    
    init(id: UUID = UUID(),
         body: String,
         createdAt: Date = Date(),
         modifiedAt: Date = Date(),
         author: UUID,
         authorName: String,
         sldId: UUID,
         x: Float,
         y: Float,
         width: Int,
         height: Int) {
        self.id = id
        self.body = body
        self.createdAt = createdAt
        self.modifiedAt = modifiedAt
        self.author = author
        self.authorName = authorName
        self.sldId = sldId
        self.x = x
        self.y = y
        self.width = width
        self.height = height
    }
    
    convenience init(from dto: SLDCommentDTO) {
        self.init(
            id: dto.id,
            body: dto.body,
            createdAt: dto.created_at,
            modifiedAt: dto.modified_at,
            author: dto.author,
            authorName: dto.author_name,
            sldId: dto.sld_id,
            x: dto.x,
            y: dto.y,
            width: dto.width,
            height: dto.height
        )
    }
    
    func toDTO() -> SLDCommentDTO {
        return SLDCommentDTO(
            id: id,
            body: body,
            created_at: createdAt,
            modified_at: modifiedAt,
            author: author,
            author_name: authorName,
            sld_id: sldId,
            x: x,
            y: y,
            width: width,
            height: height
        )
    }
}