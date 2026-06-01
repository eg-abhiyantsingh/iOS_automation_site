//
//  TaskTypeSelectionView.swift
//  SwiftDataTutorial
//
//  Task type selection popup for choosing between Simple and Complex tasks
//

import SwiftUI

struct TaskTypeSelectionView: View {
    @Binding var isPresented: Bool
    let onSimpleSelected: () -> Void
    let onComplexSelected: () -> Void

    var body: some View {
        ZStack {
            // Background overlay
            Color.black.opacity(0.4)
                .ignoresSafeArea()
                .onTapGesture {
                    isPresented = false
                }

            // Popup content
            VStack(spacing: 20) {
                // Header
                VStack(spacing: 8) {
                    Text(AppStrings.Sessions.createTask)
                        .font(.title2)
                        .fontWeight(.bold)

                    Text(AppStrings.Sessions.chooseTaskType)
                        .font(.subheadline)
                        .foregroundColor(.secondary)
                }
                .padding(.top)

                Divider()

                // Task type options
                VStack(spacing: 12) {
                    // Simple Task Option
                    Button(action: {
                        isPresented = false
                        onSimpleSelected()
                    }) {
                        HStack(spacing: 16) {
                            // Icon
                            ZStack {
                                Circle()
                                    .fill(Color.blue.opacity(0.1))
                                    .frame(width: 50, height: 50)

                                Image(systemName: "1.circle.fill")
                                    .font(.title2)
                                    .foregroundColor(.blue)
                            }

                            // Text
                            VStack(alignment: .leading, spacing: 4) {
                                Text(AppStrings.Sessions.simpleTask)
                                    .font(.headline)
                                    .foregroundColor(.primary)

                                Text(AppStrings.Sessions.oneTaskOneAsset)
                                    .font(.caption)
                                    .foregroundColor(.secondary)
                                    .multilineTextAlignment(.leading)
                            }

                            Spacer()

                            Image(systemName: "chevron.right")
                                .font(.caption)
                                .foregroundColor(.secondary)
                        }
                        .padding()
                        .background(Color(UIColor.secondarySystemBackground))
                        .cornerRadius(12)
                    }
                    .buttonStyle(.plain)

                    // Complex Task Option
                    Button(action: {
                        isPresented = false
                        onComplexSelected()
                    }) {
                        HStack(spacing: 16) {
                            // Icon
                            ZStack {
                                Circle()
                                    .fill(Color.orange.opacity(0.1))
                                    .frame(width: 50, height: 50)

                                Image(systemName: "square.stack.3d.up.fill")
                                    .font(.title2)
                                    .foregroundColor(.orange)
                            }

                            // Text
                            VStack(alignment: .leading, spacing: 4) {
                                Text(AppStrings.Sessions.complexTask)
                                    .font(.headline)
                                    .foregroundColor(.primary)

                                Text(AppStrings.Sessions.oneTaskMultipleAssets)
                                    .font(.caption)
                                    .foregroundColor(.secondary)
                                    .multilineTextAlignment(.leading)
                            }

                            Spacer()

                            Image(systemName: "chevron.right")
                                .font(.caption)
                                .foregroundColor(.secondary)
                        }
                        .padding()
                        .background(Color(UIColor.secondarySystemBackground))
                        .cornerRadius(12)
                    }
                    .buttonStyle(.plain)
                }
                .padding(.horizontal)

                // Cancel button
                Button(action: {
                    isPresented = false
                }) {
                    Text(AppStrings.Common.cancel)
                        .font(.body)
                        .foregroundColor(.secondary)
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 12)
                }
                .padding(.horizontal)
                .padding(.bottom)
            }
            .frame(maxWidth: 340)
            .background(Color(UIColor.systemBackground))
            .cornerRadius(20)
            .shadow(color: Color.black.opacity(0.2), radius: 20, y: 10)
            .padding()
        }
    }
}
