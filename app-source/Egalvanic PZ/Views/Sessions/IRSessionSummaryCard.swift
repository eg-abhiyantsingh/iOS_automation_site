//
//  IRSessionSummaryCard.swift
//  SwiftDataTutorial
//
//  Created by Eric Ehlert on 8/2/25.
//

import SwiftUI
import SwiftData

struct IRSessionSummaryCard: View {
    let diagram: SLDV2
    @Query private var irSessions: [IRSession]
    
    init(diagram: SLDV2) {
        self.diagram = diagram
        let sldId = diagram.id
        
        _irSessions = Query(
            filter: #Predicate<IRSession> { session in
                session.sld.id == sldId
            }
        )
    }
    
    private var nonDeletedSessions: [IRSession] {
        irSessions.filter { !$0.is_deleted }
    }

    private var activeSessions: [IRSession] {
        nonDeletedSessions.filter { $0.active }
    }

    private var totalPhotoCount: Int {
        nonDeletedSessions.reduce(0) { sum, session in
            sum + session.ir_photos.filter { !$0.is_deleted }.count
        }
    }
    
    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack {
                Image(systemName: "camera.metering.unknown")
                    .font(.title2)
                    .foregroundColor(.orange)
                
                VStack(alignment: .leading, spacing: 2) {
                    Text(AppStrings.Sessions.irSessions)
                        .font(.headline)
                    if !activeSessions.isEmpty {
                        Text(AppStrings.Sessions.activeCount(activeSessions.count))
                            .font(.subheadline)
                            .foregroundColor(.green)
                    } else {
                        Text(AppStrings.Sessions.totalCount(nonDeletedSessions.count))
                            .font(.subheadline)
                            .foregroundColor(.secondary)
                    }
                }
                
                Spacer()
                
                VStack(alignment: .trailing, spacing: 4) {
                    Text("\(totalPhotoCount)")
                        .font(.title)
                        .fontWeight(.semibold)
                    Text(AppStrings.Sessions.photos)
                        .font(.caption)
                        .foregroundColor(.secondary)
                }
            }
            
            if !activeSessions.isEmpty {
                Divider()
                
                VStack(alignment: .leading, spacing: 4) {
                    ForEach(activeSessions.prefix(2)) { session in
                        HStack {
                            Circle()
                                .fill(Color.green)
                                .frame(width: 8, height: 8)
                            Text(session.photo_type)
                                .font(.caption)
                                .lineLimit(1)
                            Spacer()
                            Text(AppStrings.Sessions.photoLabel(session.ir_photos.filter { !$0.is_deleted }.count))
                                .font(.caption)
                                .foregroundColor(.secondary)
                        }
                    }
                    
                    if activeSessions.count > 2 {
                        Text(AppStrings.Sessions.moreActive(activeSessions.count - 2))
                            .font(.caption)
                            .foregroundColor(.secondary)
                            .italic()
                    }
                }
            }
        }
        .padding()
        .background(Color(UIColor.systemBackground))
        .cornerRadius(12)
        .shadow(radius: 2)
    }
}
