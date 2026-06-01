import SwiftUI
import SwiftData

struct UnifiedIssueListView: View {
    @Environment(\.modelContext) private var modelContext
    @EnvironmentObject var appState: AppStateManager
    @EnvironmentObject var networkState: NetworkState
    
    @Query(filter: #Predicate<Issue> { !$0.is_deleted }) private var allIssues: [Issue]
    @State private var selectedIssue: Issue?
    @State private var showingIssueCreation = false
    @State private var issueToDelete: Issue?
    @State private var showingDeleteConfirmation = false

    var body: some View {
        EntityListView(
            configuration: IssueListConfiguration(
                appState: appState,
                networkState: networkState,
                modelContext: modelContext,
                onIssueTapped: { issue in
                    selectedIssue = issue
                },
                onCreateTapped: {
                    showingIssueCreation = true
                },
                onDeleteIssue: { issue in
                    issueToDelete = issue
                    showingDeleteConfirmation = true
                },
                onResolveIssue: { issue in
                    resolveIssue(issue)
                },
                onReopenIssue: { issue in
                    reopenIssue(issue)
                }
            ),
            entities: allIssues
        )
        .environmentObject(networkState)
        .fullScreenCover(item: $selectedIssue) { issue in
            IssueDetailView(issue: issue, stagedIRPhotos: [])
        }
        .sheet(isPresented: $showingIssueCreation) {
            UnifiedIssueCreationFromListView { issue in
                // Issue was created successfully
                AppLogger.log(.info, "Created issue: \(issue.title ?? "Untitled")", category: .issue)
            }
        }
        .alert(AppStrings.Issues.deleteIssue, isPresented: $showingDeleteConfirmation) {
            Button(AppStrings.Common.cancel, role: .cancel) {
                issueToDelete = nil
            }
            Button(AppStrings.Common.delete, role: .destructive) {
                if let issue = issueToDelete {
                    deleteIssue(issue)
                }
                issueToDelete = nil
            }
        } message: {
            Text(AppStrings.Issues.deleteIssueConfirm)
        }
    }
    
    private func resolveIssue(_ issue: Issue) {
        IssueService.resolveIssue(
            issue,
            modelContext: modelContext
        ) { success, message in
            if let errorMessage = message {
                AppLogger.log(.notice, errorMessage, category: .issue)
            }
        }
    }

    private func reopenIssue(_ issue: Issue) {
        IssueService.reopenIssue(
            issue,
            modelContext: modelContext
        ) { success, message in
            if let errorMessage = message {
                AppLogger.log(.notice, errorMessage, category: .issue)
            }
        }
    }

    private func deleteIssue(_ issue: Issue) {
        IssueService.deleteIssue(
            issue,
            modelContext: modelContext
        ) { success, message in
            if let errorMessage = message {
                AppLogger.log(.notice, errorMessage, category: .issue)
            }
        }
    }
}