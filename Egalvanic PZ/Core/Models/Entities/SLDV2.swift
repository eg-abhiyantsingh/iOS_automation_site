//
//  SLDV2.swift
//  SwiftDataTutorial
//
//  Created by Eric Ehlert on 7/14/25.
//
import SwiftUI
import SwiftData

@Model
final class SLDV2 {
    @Attribute(.unique) var id: UUID
    var name: String
    var nodes: [NodeV2]
    var edges: [EdgeV2]
    var photos: [Photo]
    var ir_photos: [IRPhoto]
    var ir_sessions: [IRSession] = []
    var user_tasks: [UserTask] = []
    var issues: [Issue] = []
    var quotes: [Quote] = []
    var comments: [SLDComment] = []
    @Relationship(inverse: \Building.sld) var buildings: [Building] = []

    // Address fields
    var address_line_1: String?
    var address_line_2: String?
    var city: String?
    var state_province: String?
    var postal_code: String?
    var country_code: String?
    var address_formatted: String?
    var address_latitude: Double?
    var address_longitude: Double?

    init(id: UUID, name: String, nodes: [NodeV2], edges: [EdgeV2], photos: [Photo], ir_photos: [IRPhoto], ir_sessions: [IRSession], user_tasks: [UserTask], issues: [Issue], quotes: [Quote], comments: [SLDComment] = [], address_line_1: String? = nil, address_line_2: String? = nil, city: String? = nil, state_province: String? = nil, postal_code: String? = nil, country_code: String? = nil, address_formatted: String? = nil, address_latitude: Double? = nil, address_longitude: Double? = nil) {
        self.id = id
        self.name = name
        self.nodes = nodes
        self.edges = edges
        self.photos = []
        self.ir_photos = []
        self.ir_sessions = []
        self.user_tasks = []
        self.issues = []
        self.quotes = []
        self.comments = comments
        self.address_line_1 = address_line_1
        self.address_line_2 = address_line_2
        self.city = city
        self.state_province = state_province
        self.postal_code = postal_code
        self.country_code = country_code
        self.address_formatted = address_formatted
        self.address_latitude = address_latitude
        self.address_longitude = address_longitude
    }
    
    func toDTO() -> SLDDTO {
        SLDDTO(
            id: self.id,
            name: self.name,
            nodes: nodes
                .filter { !$0.is_deleted }
                .map { node in
                    SLDDTONode(
                        id: node.id,
                        type: node.type,
                        label: node.label,
                        sld_id: self.id,
                        parent_id: node.parent_id,
                        x: node.x,
                        y: node.y,
                        width: node.width,
                        height: node.height,
                        node_class: node.node_class?.id,
                        core_attributes: node.core_attributes.map { attr in
                            NodePropertyDTO(
                                id: attr.id,
                                node_class_property: attr.node_class_property?.id.uuidString ?? "",
                                name: attr.name,
                                value: attr.value
                            )
                        },
                        af_completion: node.af_isComplete
                    )
                },
            edges: edges
                .filter { !$0.is_deleted }
                .map { edge in
                    SLDDTOEdge(
                        id: edge.id,
                        source: edge.source,
                        target: edge.target,
                        sld_id: self.id,
                        core_attributes: edge.core_attributes.map { attr in
                            EdgePropertyDTO(
                                id: attr.id,
                                edge_class_property: attr.edge_class_property?.id.uuidString ?? "",
                                name: attr.name,
                                value: attr.value
                            )
                        },
                        af_completion: edge.af_isComplete,
                        points: edge.points,
                        algorithm: edge.algorithm
                    )
                },
            photos: [],
            tasks: [],
            ir_photos: [],
            ir_sessions: [],
            issues: [],
            quotes: [],
            comments: comments.map { $0.toDTO() }
        )
    }
}