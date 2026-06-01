import SwiftUI
import SwiftData

struct IRSessionPickerButton: View {
    @EnvironmentObject var appState: AppStateManager
    @Environment(\.modelContext) private var modelContext
    @State private var activeSessions: [IRSession] = []
    @State private var showingStartAlert: Bool = false
    @State private var showingEndAlert: Bool = false
    @State private var sessionToStart: IRSession?

    var body: some View {
        Menu {
            sessionMenuContent
        } label: {
            ZStack {
                RoundedRectangle(cornerRadius: 4)
                    .fill(appState.activeSession != nil ? Color.blue : Color.gray.opacity(0.4))
                    .frame(width: 26, height: 18)
                Text(AppStrings.Forms.wo)
                    .font(.system(size: 9, weight: .bold))
                    .foregroundColor(.white)
            }
        }
        .onAppear {
            loadActiveSessions()
        }
        .onChange(of: appState.activeSession) { _ in
            loadActiveSessions()
        }
        .alert(AppStrings.Sessions.startWorkOrderSession, isPresented: $showingStartAlert) {
            Button(AppStrings.Common.cancel, role: .cancel) {
                sessionToStart = nil
            }
            Button(AppStrings.Sessions.startSession) {
                if let session = sessionToStart {
                    appState.setActiveSession(session)
                }
                sessionToStart = nil
            }
        } message: {
            if let session = sessionToStart {
                Text(AppStrings.Sessions.startSessionConfirm(name: session.name ?? AppStrings.Supporting.unnamedSession))
            }
        }
        .alert(AppStrings.Sessions.endWorkOrderSession, isPresented: $showingEndAlert) {
            Button(AppStrings.Common.cancel, role: .cancel) { }
            Button(AppStrings.Sessions.endSession, role: .destructive) {
                appState.setActiveSession(nil)
            }
        } message: {
            if let session = appState.activeSession {
                Text(AppStrings.Sessions.endSessionConfirm(name: session.name ?? AppStrings.Supporting.unnamedSession))
            }
        }
    }

    @ViewBuilder
    private var sessionMenuContent: some View {
        if activeSessions.isEmpty {
            Text(AppStrings.Supporting.noActiveSessions)
                .foregroundColor(.secondary)
        } else {
            // Option to end current session
            if appState.activeSession != nil {
                Button {
                    showingEndAlert = true
                } label: {
                    Label(AppStrings.Sessions.endSession, systemImage: "stop.circle")
                }
                
                Divider()
            }

            ForEach(activeSessions, id: \.id) { session in
                sessionButton(for: session)
            }
        }
    }

    private func sessionButton(for session: IRSession) -> some View {
        Button {
            if appState.activeSession?.id == session.id {
                showingEndAlert = true
            } else {
                sessionToStart = session
                showingStartAlert = true
            }
        } label: {
            Label {
                Text(session.name ?? AppStrings.Supporting.unnamedSession)
            } icon: {
                Image(systemName: appState.activeSession?.id == session.id ? "checkmark.circle.fill" : "circle")
            }
        }
    }

    private func loadActiveSessions() {
        let descriptor = FetchDescriptor<IRSession>(
            predicate: #Predicate<IRSession> { session in
                session.active == true && session.is_deleted == false
            }
        )

        do {
            activeSessions = try modelContext.fetch(descriptor)
        } catch {
            AppLogger.log(.error, "Failed to fetch active IR sessions: \(error)", category: .ui)
            activeSessions = []
        }
    }
}
