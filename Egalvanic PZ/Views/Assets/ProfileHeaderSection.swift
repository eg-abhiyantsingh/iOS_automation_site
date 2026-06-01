//
//  ProfileHeaderSection.swift
//  SwiftDataTutorial
//
//  Created by Eric Ehlert on 8/7/25.
//

import SwiftUI

struct ProfileHeaderSection: View {
    let profilePhoto: Photo?
    let nodeLabel: String
    let nodeType: String
    let location: String?
    let com: Int?

    private var comColor: Color {
        guard let com else { return .gray }
        switch com {
        case 1: return .green
        case 2: return .yellow
        case 3: return .red
        default: return .gray
        }
    }
    
    private var comLabel: String {
        guard let com else { return "N/S" }
        switch com {
        case 1: return "1"
        case 2: return "2"
        case 3: return "3"
        default: return AppStrings.AssetsExtra.unknown
        }
    }
    
    var body: some View {
        VStack(spacing: 16) {
            // Profile Image with COM indicator
            ZStack(alignment: .topTrailing) {
                // Profile Image
                Group {
                    if let photo = profilePhoto {
                        // Check if we have a display URL (either local or remote)
                        if photo.isLocallyAvailable, let localURL = photo.localFileURL {
                            // Prefer local photo
                            AsyncImage(url: localURL) { image in
                                image
                                    .resizable()
                                    .aspectRatio(contentMode: .fill)
                                    .frame(width: 120, height: 120)
                                    .clipShape(Circle())
                                    .overlay(Circle().stroke(Color.white, lineWidth: 4))
                                    .shadow(radius: 10)
                            } placeholder: {
                                ProgressView()
                                    .frame(width: 120, height: 120)
                                    .background(Color(.systemGray5))
                                    .clipShape(Circle())
                                    .overlay(Circle().stroke(Color.white, lineWidth: 4))
                                    .shadow(radius: 10)
                            }
                        } else if photo.url != nil {
                            // Use remote photo with presigned URL (works offline if cached)
                            PresignedPhotoImage(
                                photo: photo,
                                content: { image in
                                    image
                                        .resizable()
                                        .aspectRatio(contentMode: .fill)
                                        .frame(width: 120, height: 120)
                                        .clipShape(Circle())
                                        .overlay(Circle().stroke(Color.white, lineWidth: 4))
                                        .shadow(radius: 10)
                                },
                                placeholder: {
                                    ProgressView()
                                        .frame(width: 120, height: 120)
                                        .background(Color(.systemGray5))
                                        .clipShape(Circle())
                                        .overlay(Circle().stroke(Color.white, lineWidth: 4))
                                        .shadow(radius: 10)
                                },
                                onFailure: {
                                    // Show photo icon when load fails (no internet/not cached)
                                    Image(systemName: "photo.circle.fill")
                                        .font(.system(size: 60))
                                        .foregroundColor(.gray)
                                        .frame(width: 120, height: 120)
                                        .background(Color(.systemGray5))
                                        .clipShape(Circle())
                                        .overlay(Circle().stroke(Color.white, lineWidth: 4))
                                        .shadow(radius: 10)
                                },
                                retryButtonAlignment: .center
                            )
                        } else {
                            // No available photo
                            Image(systemName: "photo.circle.fill")
                                .font(.system(size: 60))
                                .foregroundColor(.gray)
                                .frame(width: 120, height: 120)
                                .background(Color(.systemGray5))
                                .clipShape(Circle())
                                .overlay(Circle().stroke(Color.white, lineWidth: 4))
                                .shadow(radius: 10)
                        }
                    } else {
                        // No photo at all
                        Image(systemName: "cube.fill")
                            .font(.system(size: 60))
                            .foregroundColor(.blue)
                            .frame(width: 120, height: 120)
                            .background(Color.blue.opacity(0.1))
                            .clipShape(Circle())
                            .overlay(Circle().stroke(Color.white, lineWidth: 4))
                            .shadow(radius: 10)
                    }
                }
                
                // COM Badge
                Circle()
                    .fill(comColor)
                    .frame(width: 36, height: 36)
                    .overlay(
                        Text(comLabel)
                            .font(.caption)
                            .fontWeight(.bold)
                            .foregroundColor(com == 2 ? .black : .white)
                    )
                    .overlay(Circle().stroke(Color.white, lineWidth: 2))
                    .offset(x: -15, y: 15)
            }
            
            // Node Info
            VStack(spacing: 4) {
                Text(nodeLabel)
                    .font(.title2)
                    .fontWeight(.bold)
                
                Text(nodeType)
                    .font(.subheadline)
                    .foregroundColor(.secondary)
                
                if let location = location {
                    Label(location, systemImage: "location.fill")
                        .font(.caption)
                        .foregroundColor(.blue)
                }
            }
        }
        .padding(.top, 20)
        .padding(.bottom, 30)
        .frame(maxWidth: .infinity)
        .background(
            LinearGradient(
                colors: [Color.blue.opacity(0.1), Color.clear],
                startPoint: .top,
                endPoint: .bottom
            )
        )
    }
}
