//
//  NodeTypeIcon.swift
//  Egalvanic PZ
//
//  Created by Claude on 12/1/25.
//
//  A reusable component that displays the appropriate icon for a node based on its NodeClass style.
//  Icons are loaded from the NodeTypeIcons asset catalog.
//

import SwiftUI
import UIKit

/// A view that displays the appropriate icon for a node based on its NodeClass style.
/// Uses custom SVG icons from the asset catalog.
struct NodeTypeIcon: View {
    let style: String?
    var size: CGFloat = 20
    var color: Color = .secondary

    /// Maps a NodeClass style string to the corresponding asset catalog image name.
    /// Returns nil if no specific icon exists for the style.
    private var iconAssetName: String? {
        guard let style = style?.lowercased() else { return nil }
        switch style {
        case "ats", "automatic transfer switch":
            return "ats"
        case "busway", "bus", "busbar":
            return "busway"
        case "circuitbreaker", "circuit breaker", "breaker", "cb":
            return "circuitBreaker"
        case "electricalpanel", "electrical panel", "panel", "switchboard", "switchgear", "mcc":
            return "electricalPanel"
        case "fuse":
            return "fuse"
        case "generator", "gen":
            return "generator"
        case "junctionbox", "junction box", "junction", "jbox":
            return "junctionBox"
        case "motor", "mtr":
            return "motor"
        case "relay":
            return "relay"
        case "switch", "sw", "disconnect":
            return "switch"
        case "transformer", "xfmr", "xfrm":
            return "transformer"
        case "ups", "uninterruptible power supply":
            return "ups"
        case "utility", "util", "service entrance", "main":
            return "utility"
        default:
            return nil
        }
    }

    var body: some View {
        if let assetName = iconAssetName,
           let uiImage = UIImage(named: assetName) {
            Image(uiImage: uiImage)
                .renderingMode(.template)
                .resizable()
                .aspectRatio(contentMode: .fit)
                .frame(width: size, height: size)
                .foregroundColor(color)
        } else {
            // Fallback to default icon
            Image("default")
                .renderingMode(.template)
                .resizable()
                .aspectRatio(contentMode: .fit)
                .frame(width: size, height: size)
                .foregroundColor(color)
        }
    }
}

/// A variant that shows the icon with a circular background
struct NodeTypeIconCircle: View {
    let style: String?
    var size: CGFloat = 44
    var iconSize: CGFloat = 20
    var backgroundColor: Color = Color.blue.opacity(0.1)
    var iconColor: Color = .blue

    var body: some View {
        ZStack {
            Circle()
                .fill(backgroundColor)
                .frame(width: size, height: size)

            NodeTypeIcon(
                style: style,
                size: iconSize,
                color: iconColor
            )
        }
    }
}

#Preview {
    VStack(spacing: 16) {
        HStack(spacing: 16) {
            NodeTypeIcon(style: "transformer", size: 30, color: .blue)
            NodeTypeIcon(style: "generator", size: 30, color: .green)
            NodeTypeIcon(style: "ats", size: 30, color: .orange)
            NodeTypeIcon(style: "panel", size: 30, color: .purple)
        }

        HStack(spacing: 16) {
            NodeTypeIconCircle(style: "transformer")
            NodeTypeIconCircle(style: "breaker")
            NodeTypeIconCircle(style: "motor")
            NodeTypeIconCircle(style: nil) // Should show default
        }
    }
    .padding()
}
