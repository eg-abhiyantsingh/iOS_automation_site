//
//  QuotesView.swift
//  SwiftDataTutorial
//
//  Created by Eric Ehlert on 8/3/25.
//

import SwiftUI
import SwiftData

struct QuotesView: View {
    @Environment(\.modelContext) private var modelContext
    @Environment(\.dismiss) private var dismiss
    @EnvironmentObject var networkState: NetworkState
    
    let diagram: SLDV2
    
    // Capture the diagram ID for use in predicates
    private var diagramId: UUID { diagram.id }
    
    @Query private var quotes: [Quote]
    
    @State private var selectedQuote: Quote?
    @State private var showingQuoteDetail = false
    @State private var searchText = ""
    @State private var filterStatus: QuoteFilterStatus = .all
    
    init(diagram: SLDV2) {
        self.diagram = diagram
        let sldId = diagram.id
        
        // Query all quotes for this SLD
        _quotes = Query(
            filter: #Predicate<Quote> { quote in
                quote.sld?.id == sldId && !quote.is_deleted
            },
            sort: [SortDescriptor(\Quote.modified_date, order: .reverse)]
        )
    }
    
    enum QuoteFilterStatus: String, CaseIterable {
        case all = "All"
        case draft = "Draft"
        case pending = "Pending"
        case approved = "Approved"
        case rejected = "Rejected"
        
        var systemImage: String {
            switch self {
            case .all: return "list.bullet"
            case .draft: return "doc.text"
            case .pending: return "clock"
            case .approved: return "checkmark.circle"
            case .rejected: return "xmark.circle"
            }
        }
        
        var color: Color {
            switch self {
            case .all: return .gray
            case .draft: return .gray
            case .pending: return .orange
            case .approved: return .green
            case .rejected: return .red
            }
        }
    }
    
    private var filteredQuotes: [Quote] {
        quotes.filter { quote in
            // Status filter
            let matchesStatus = filterStatus == .all || quote.status?.lowercased() == filterStatus.rawValue.lowercased()
            
            // Search filter
            let matchesSearch = searchText.isEmpty ||
                (quote.title?.localizedCaseInsensitiveContains(searchText) ?? false) ||
                (quote.quoteDescription?.localizedCaseInsensitiveContains(searchText) ?? false)
            
            return matchesStatus && matchesSearch
        }
    }
    
    private var totalValue: Double {
        // If sow/tnm are JSON strings with value fields, you'd parse them here
        // For now, returning a placeholder
        0.0
    }
    
    private func statusColor(for status: String?) -> Color {
        switch status?.lowercased() {
        case "draft":
            return .gray
        case "pending":
            return .orange
        case "approved":
            return .green
        case "rejected":
            return .red
        default:
            return .gray
        }
    }
    
    private var dateFormatter: DateFormatter {
        let formatter = DateFormatter()
        formatter.dateStyle = .medium
        formatter.timeStyle = .none
        return formatter
    }
    
    var body: some View {
        NavigationView {
            VStack(spacing: 0) {
                // Search and Filter Bar
                VStack(spacing: 12) {
                    // Search
                    HStack {
                        Image(systemName: "magnifyingglass")
                            .foregroundColor(.gray)
                        TextField(AppStrings.Quotes.searchQuotes, text: $searchText)
                            .textFieldStyle(.plain)
                    }
                    .padding(10)
                    .background(Color(UIColor.systemGray6))
                    .cornerRadius(10)
                    
                    // Filter Pills
                    ScrollView(.horizontal, showsIndicators: false) {
                        HStack(spacing: 12) {
                            ForEach(QuoteFilterStatus.allCases, id: \.self) { status in
                                FilterPill(
                                    title: status.rawValue,
                                    icon: status.systemImage,
                                    isSelected: filterStatus == status,
                                    color: status.color,
                                    count: quotes.filter { quote in
                                        status == .all || quote.status?.lowercased() == status.rawValue.lowercased()
                                    }.count
                                ) {
                                    filterStatus = status
                                }
                            }
                        }
                    }
                }
                .padding()
                .background(Color(UIColor.systemBackground))
                
                // Summary Card
                HStack(spacing: 20) {
                    VStack(alignment: .leading, spacing: 4) {
                        Text(AppStrings.Quotes.totalQuotes)
                            .font(.caption)
                            .foregroundColor(.secondary)
                        Text("\(quotes.count)")
                            .font(.title2)
                            .fontWeight(.semibold)
                    }
                    
                    Spacer()
                    
                    VStack(alignment: .leading, spacing: 4) {
                        Text(AppStrings.Quotes.approved)
                            .font(.caption)
                            .foregroundColor(.secondary)
                        Text("\(quotes.filter { $0.status?.lowercased() == "approved" }.count)")
                            .font(.title2)
                            .fontWeight(.semibold)
                            .foregroundColor(.green)
                    }
                    
                    Spacer()
                    
                    VStack(alignment: .leading, spacing: 4) {
                        Text(AppStrings.Common.pending)
                            .font(.caption)
                            .foregroundColor(.secondary)
                        Text("\(quotes.filter { $0.status?.lowercased() == "pending" }.count)")
                            .font(.title2)
                            .fontWeight(.semibold)
                            .foregroundColor(.orange)
                    }
                }
                .padding()
                .background(Color(UIColor.systemGray6))
                .cornerRadius(12)
                .padding(.horizontal)
                
                // Quotes List
                if filteredQuotes.isEmpty {
                    ContentUnavailableView(
                        searchText.isEmpty ? AppStrings.Quotes.noQuotes : AppStrings.Quotes.noResults,
                        systemImage: "doc.text.magnifyingglass",
                        description: Text(searchText.isEmpty ? AppStrings.Quotes.quotesWillAppear : AppStrings.Quotes.tryAdjustingSearch)
                    )
                    .frame(maxHeight: .infinity)
                } else {
                    List {
                        ForEach(filteredQuotes) { quote in
                            QuoteRow(quote: quote)
                                .onTapGesture {
                                    selectedQuote = quote
                                    showingQuoteDetail = true
                                }
                        }
                    }
                    .listStyle(.plain)
                }
            }
            .navigationTitle(AppStrings.Quotes.quotes)
            .navigationBarTitleDisplayMode(.large)
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    Button(AppStrings.Common.done) {
                        dismiss()
                    }
                }
            }
        }
        .sheet(item: $selectedQuote) { quote in
            QuoteDetailView(quote: quote)
                .environmentObject(networkState)
        }
    }
}

struct FilterPill: View {
    let title: String
    let icon: String
    let isSelected: Bool
    let color: Color
    let count: Int
    let action: () -> Void
    
    var body: some View {
        Button(action: action) {
            HStack(spacing: 6) {
                Image(systemName: icon)
                    .font(.system(size: 14))
                Text(title)
                    .font(.system(size: 14, weight: .medium))
                if count > 0 {
                    Text("(\(count))")
                        .font(.system(size: 12))
                }
            }
            .foregroundColor(isSelected ? .white : color)
            .padding(.horizontal, 16)
            .padding(.vertical, 8)
            .background(isSelected ? color : Color(UIColor.systemGray6))
            .cornerRadius(20)
        }
    }
}

struct QuoteRow: View {
    let quote: Quote
    
    private var statusColor: Color {
        switch quote.status?.lowercased() {
        case "draft":
            return .gray
        case "pending":
            return .orange
        case "approved":
            return .green
        case "rejected":
            return .red
        default:
            return .gray
        }
    }
    
    private var dateFormatter: DateFormatter {
        let formatter = DateFormatter()
        formatter.dateStyle = .medium
        return formatter
    }
    
    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack {
                VStack(alignment: .leading, spacing: 4) {
                    Text(quote.title ?? AppStrings.Quotes.untitledQuote)
                        .font(.headline)
                        .lineLimit(1)

                    if let description = quote.quoteDescription {
                        Text(description)
                            .font(.subheadline)
                            .foregroundColor(.secondary)
                            .lineLimit(2)
                    }
                }
                
                Spacer()
                
                VStack(alignment: .trailing, spacing: 4) {
                    if let status = quote.status {
                        Text(LanguageManager.localizedStatus(status))
                            .font(.caption)
                            .fontWeight(.semibold)
                            .foregroundColor(.white)
                            .padding(.horizontal, 10)
                            .padding(.vertical, 4)
                            .background(statusColor)
                            .cornerRadius(12)
                    }
                    
                    if let modifiedDate = quote.modified_date {
                        Text(dateFormatter.string(from: modifiedDate))
                            .font(.caption)
                            .foregroundColor(.secondary)
                    }
                }
            }
            
            HStack(spacing: 16) {
                Label("\(quote.tasks.count) tasks", systemImage: "checklist")
                    .font(.caption)
                    .foregroundColor(.secondary)
                
                if let sow = quote.sow, !sow.isEmpty {
                    Label(AppStrings.Quotes.sow, systemImage: "doc.text")
                        .font(.caption)
                        .foregroundColor(.blue)
                }
                
                if let tnm = quote.tnm, !tnm.isEmpty {
                    Label(AppStrings.Quotes.tm, systemImage: "dollarsign.circle")
                        .font(.caption)
                        .foregroundColor(.green)
                }
                
                Spacer()
                
                Image(systemName: "chevron.right")
                    .font(.system(size: 12))
                    .foregroundColor(.gray)
            }
        }
        .padding(.vertical, 8)
    }
}

// Placeholder for Quote Detail View
struct QuoteDetailView: View {
    let quote: Quote
    @Environment(\.dismiss) private var dismiss
    @EnvironmentObject var networkState: NetworkState
    
    var body: some View {
        NavigationView {
            ScrollView {
                VStack(alignment: .leading, spacing: 20) {
                    // Quote header
                    VStack(alignment: .leading, spacing: 8) {
                        Text(quote.title ?? AppStrings.Quotes.untitledQuote)
                            .font(.largeTitle)
                            .fontWeight(.bold)
                        
                        if let description = quote.quoteDescription {
                            Text(description)
                                .font(.body)
                                .foregroundColor(.secondary)
                        }
                    }
                    .padding()
                    
                    // Add more quote details here
                    Text(AppStrings.Quotes.quoteDetailsPlaceholder)
                        .padding()
                        .foregroundColor(.secondary)
                }
            }
            .scrollDismissesKeyboard(.interactively)
            .navigationTitle(AppStrings.Quotes.quoteDetails)
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    Button(AppStrings.Common.done) {
                        dismiss()
                    }
                }
            }
        }
    }
}
