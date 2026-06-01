//
//  SLDDTO.swift
//  SwiftDataTutorial
//
//  Main SLD Data Transfer Object
//
import Foundation

struct SLDDTO: Codable {
    var id: UUID
    var name: String
    var nodes: [SLDDTONode]
    var edges: [SLDDTOEdge]
    var photos: [SLDDTOPhoto]
    var tasks: [UserTaskDTO]
    var ir_photos: [IRPhotoDTO]
    var ir_sessions: [IRSessionDTO]
    var issues: [IssueDTO]
    var quotes: [QuoteDTO]
    var comments: [SLDCommentDTO]
    var form_instances: [FormInstanceDTO]?
    var eg_forms: [EGFormDTO]?
    var eg_form_instances: [EGFormInstanceDTO]?
    var attachments: [AttachmentDTO]?
    var mappings: SLDMappingsDTO?

    // SLD Views
    var sld_views: [SLDViewDTO]?
    var sld_links: [SLDLinkDTO]?

    // Location hierarchy
    var buildings: [BuildingDTO]?
    var floors: [FloorDTO]?
    var rooms: [RoomDTO]?

    // Address fields
    var address_line_1: String?
    var address_line_2: String?
    var city: String?
    var state_province: String?
    var postal_code: String?
    var country_code: String?
    var address_formatted: String?
    var address_coordinates: [String: Double]?
}