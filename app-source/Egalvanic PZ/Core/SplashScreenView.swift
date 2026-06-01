//
//  SplashScreenView.swift
//  SwiftDataTutorial
//
//  Splash screen with animated logo
//

import SwiftUI

struct SplashScreenView: View {
    @State private var logoScale: CGFloat = 0.7
    @State private var logoOpacity: Double = 0.0
    @State private var showingGradient: Bool = false
    @State private var animationCompleted: Bool = false
    
    let onAnimationComplete: () -> Void
    
    var body: some View {
        ZStack {
            // Background gradient
            LinearGradient(
                colors: showingGradient ? [
                    Color.blue.opacity(0.1),
                    Color.purple.opacity(0.05),
                    Color.clear
                ] : [Color.clear],
                startPoint: .topLeading,
                endPoint: .bottomTrailing
            )
            .ignoresSafeArea()
            .animation(.easeInOut(duration: 1.5), value: showingGradient)
            
            VStack(spacing: 30) {
                // Logo with animations
                Image("Logo")
                    .resizable()
                    .aspectRatio(contentMode: .fit)
                    .frame(width: 120, height: 120)
                    .scaleEffect(logoScale)
                    .opacity(logoOpacity)
                    .animation(.spring(response: 1.0, dampingFraction: 0.8), value: logoScale)
                    .animation(.easeInOut(duration: 0.8), value: logoOpacity)
                
                // App name or tagline (optional)
                VStack(spacing: 8) {
                    Text("Z Platform")
                        .font(.title2)
                        .fontWeight(.semibold)
                        .foregroundStyle(.primary)
                        .opacity(logoOpacity)
                        .animation(.easeInOut(duration: 0.8).delay(0.3), value: logoOpacity)
                    
                    Text("Your Electrical Copilot")
                        .font(.caption)
                        .foregroundStyle(.secondary)
                        .opacity(logoOpacity)
                        .animation(.easeInOut(duration: 0.8).delay(0.5), value: logoOpacity)
                }
            }
        }
        .onAppear {
            startAnimations()
        }
        .task {
            // Wait for animations to complete, then transition
            try? await Task.sleep(nanoseconds: 2_000_000_000) // 2.0 seconds
            if animationCompleted {
                onAnimationComplete()
            }
        }
    }
    
    private func startAnimations() {
        // Start background gradient
        showingGradient = true
        
        // Clean, standard logo animations
        withAnimation(.easeOut(duration: 0.6)) {
            logoOpacity = 1.0
        }
        
        withAnimation(.spring(response: 0.8, dampingFraction: 0.8).delay(0.1)) {
            logoScale = 1.0
        }
        
        // Subtle scale animation for polish
        DispatchQueue.main.asyncAfter(deadline: .now() + 1.0) {
            withAnimation(.spring(response: 0.5, dampingFraction: 0.9)) {
                logoScale = 1.05
            }
        }
        
        // Scale back to normal
        DispatchQueue.main.asyncAfter(deadline: .now() + 1.3) {
            withAnimation(.spring(response: 0.5, dampingFraction: 0.9)) {
                logoScale = 1.0
            }
            animationCompleted = true
        }
    }
}

#Preview {
    SplashScreenView {
        AppLogger.log(.debug, "Splash animation completed", category: .ui)
    }
}
