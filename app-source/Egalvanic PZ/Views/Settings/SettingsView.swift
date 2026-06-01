import SwiftUI
import SwiftData

struct SettingsView: View {
    @Environment(\.modelContext) private var modelContext
    @EnvironmentObject var networkState: NetworkState
    @EnvironmentObject var appState: AppStateManager
    @EnvironmentObject var authService: AuthService  // Add this
    @EnvironmentObject var languageManager: LanguageManager
    @EnvironmentObject var sldService: SLDService
    @StateObject private var biometricService = BiometricAuthService.shared
    @State private var showingLogoutConfirmation = false
    @State private var showingLanguageConfirmation = false
    @State private var pendingLanguage: AppLanguage?
    @State private var showingLogoutError = false
    @State private var logoutErrorMessage = ""
    @State private var showingBiometricDisableAlert = false
    @State private var showingBiometricEnableSheet = false
    @State private var passwordForBiometric = ""
    @State private var biometricEnableError: String?
    @State private var isEnablingBiometric = false
    @State private var showPassword = false
    @State private var isClearingCache = false
    @State private var showCacheClearedMessage = false
    @State private var showingClearCacheConfirmation = false
    @State private var cacheSizeText = ""
    @State private var showingEqpLibraryConfirmation = false
    @State private var isDownloadingEqpLibrary = false
    @State private var eqpLibraryStatus = ""
    @State private var eqpLibraryError: String?
    @State private var eqpLibraryProgress: Double = 0
    @State private var eqpLibraryProgressLabel = ""
    // ZP-2161 Phase 4b: SKM library download (replaces EasyPower long-term).
    @State private var showingSkmLibraryConfirmation = false
    @State private var isDownloadingSkmLibrary = false
    @State private var skmLibraryStatus = ""
    @State private var skmLibraryError: String?
    @State private var skmLibraryProgress: Double = 0
    @State private var skmLibraryProgressLabel = ""
    @State private var sessionRecordingEnabled: Bool = DevRevPreferences.isRecordingEnabled
    
    var body: some View {
        ScrollView {
            VStack(spacing: 20) {
                // User Info Section (NEW)
                if let user = authService.currentUser {
                    VStack(alignment: .leading, spacing: 12) {
                        Text(AppStrings.Settings.account)
                            .font(.headline)
                            .foregroundColor(.secondary)
                            .padding(.horizontal)
                        
                        VStack(spacing: 0) {
                            if let email = user.email {
                                HStack {
                                    Text(AppStrings.Common.email)
                                        .font(.subheadline)
                                    Spacer()
                                    Text(email)
                                        .font(.subheadline)
                                        .foregroundColor(.secondary)
                                }
                                .padding(16)
                                
                                Divider()
                                    .padding(.leading, 16)
                            }
                            
                            if let name = user.given_name {
                                HStack {
                                    Text(AppStrings.Common.name)
                                        .font(.subheadline)
                                    Spacer()
                                    Text("\(name) \(user.family_name ?? "")")
                                        .font(.subheadline)
                                        .foregroundColor(.secondary)
                                }
                                .padding(16)
                                
                                Divider()
                                    .padding(.leading, 16)
                            }
                            
                            HStack {
                                Text(AppStrings.Settings.userID)
                                    .font(.subheadline)
                                Spacer()
                                Text(String(user.sub.prefix(8)) + "...")
                                    .font(.caption)
                                    .foregroundColor(.secondary)
                                    .monospaced()
                            }
                            .padding(16)
                        }
                        .background(Color(UIColor.secondarySystemBackground))
                        .cornerRadius(16)
                        .padding(.horizontal)
                    }
                }

                // Language Section
                VStack(alignment: .leading, spacing: 12) {
                    Text(AppStrings.Settings.language)
                        .font(.headline)
                        .foregroundColor(.secondary)
                        .padding(.horizontal)

                    VStack(spacing: 0) {
                        ForEach(AppLanguage.allCases, id: \.self) { language in
                            HStack {
                                Text(language.flagEmoji)
                                    .font(.title2)
                                    .frame(width: 30)

                                Text(language.displayName)
                                    .font(.subheadline)

                                Spacer()

                                if languageManager.currentLanguage == language {
                                    Image(systemName: "checkmark.circle.fill")
                                        .foregroundColor(.blue)
                                }
                            }
                            .padding(16)
                            .contentShape(Rectangle())
                            .onTapGesture {
                                if languageManager.currentLanguage != language {
                                    pendingLanguage = language
                                    showingLanguageConfirmation = true
                                }
                            }

                            if language != AppLanguage.allCases.last {
                                Divider()
                                    .padding(.leading, 16)
                            }
                        }
                    }
                    .background(Color(UIColor.secondarySystemBackground))
                    .cornerRadius(16)
                    .padding(.horizontal)
                }

                // Biometric Authentication Section
                if biometricService.canUseBiometric() {
                    VStack(alignment: .leading, spacing: 12) {
                        Text(AppStrings.Settings.security)
                            .font(.headline)
                            .foregroundColor(.secondary)
                            .padding(.horizontal)

                        VStack(spacing: 0) {
                            HStack {
                                Image(systemName: biometricService.biometricType.iconName)
                                    .font(.title2)
                                    .foregroundColor(.blue)
                                    .frame(width: 30)

                                VStack(alignment: .leading, spacing: 2) {
                                    Text(AppStrings.Settings.biometricLogin(type: biometricService.biometricType.displayName))
                                        .font(.subheadline)
                                    Text(AppStrings.Settings.useBiometricForLogin)
                                        .font(.caption)
                                        .foregroundColor(.secondary)
                                }

                                Spacer()

                                Toggle("", isOn: Binding(
                                    get: { biometricService.isBiometricAuthEnabled },
                                    set: { newValue in
                                        if newValue {
                                            // Enable biometric - show password entry sheet
                                            enableBiometric()
                                        } else {
                                            // Confirm before disabling
                                            showingBiometricDisableAlert = true
                                        }
                                    }
                                ))
                                .labelsHidden()
                            }
                            .padding(16)
                        }
                        .background(Color(UIColor.secondarySystemBackground))
                        .cornerRadius(16)
                        .padding(.horizontal)
                    }
                }

                // Session Analytics Section
                if DevRevService.shared.isConfigured {
                    VStack(alignment: .leading, spacing: 12) {
                        Text(AppStrings.Settings.sessionAnalytics)
                            .font(.headline)
                            .foregroundColor(.secondary)
                            .padding(.horizontal)

                        VStack(spacing: 0) {
                            HStack(spacing: 16) {
                                ZStack {
                                    Circle()
                                        .fill(Color.blue.opacity(0.15))
                                        .frame(width: 44, height: 44)

                                    Image(systemName: "waveform")
                                        .font(.system(size: 20))
                                        .foregroundColor(.blue)
                                }

                                VStack(alignment: .leading, spacing: 2) {
                                    Text(AppStrings.Settings.sessionRecording)
                                        .font(.subheadline)
                                    Text(AppStrings.Settings.sessionRecordingHint)
                                        .font(.caption)
                                        .foregroundColor(.secondary)
                                }

                                Spacer()

                                Toggle("", isOn: $sessionRecordingEnabled)
                                    .labelsHidden()
                            }
                            .padding(16)
                        }
                        .background(Color(UIColor.secondarySystemBackground))
                        .cornerRadius(16)
                        .padding(.horizontal)
                    }
                }

                // Sync & Network Section
                VStack(alignment: .leading, spacing: 12) {
                    Text(AppStrings.Settings.syncAndNetwork)
                        .font(.headline)
                        .foregroundColor(.secondary)
                        .padding(.horizontal)
                    
                    VStack(spacing: 12) {
                        // Sync Queue Analyzer
                        NavigationLink(destination: SyncQueueAnalyzerView()) {
                            HStack(spacing: 16) {
                                ZStack {
                                    Circle()
                                        .fill(Color.blue.opacity(0.15))
                                        .frame(width: 44, height: 44)
                                    
                                    Image(systemName: "arrow.triangle.2.circlepath")
                                        .font(.system(size: 20))
                                        .foregroundColor(.blue)
                                }
                                
                                VStack(alignment: .leading, spacing: 4) {
                                    Text(AppStrings.Settings.syncQueueAnalyzer)
                                        .font(.subheadline)
                                        .fontWeight(.medium)

                                    Text(AppStrings.Settings.managePendingSyncOps)
                                        .font(.caption)
                                        .foregroundColor(.secondary)
                                }
                                
                                Spacer()
                                
                                if networkState.syncQueueCount > 0 {
                                    ZStack {
                                        Capsule()
                                            .fill(Color.red)
                                            .frame(width: 32, height: 22)
                                        
                                        Text("\(networkState.syncQueueCount)")
                                            .font(.caption)
                                            .fontWeight(.bold)
                                            .foregroundColor(.white)
                                    }
                                }
                                
                                Image(systemName: "chevron.right")
                                    .font(.system(size: 12, weight: .semibold))
                                    .foregroundColor(.accentColor)
                            }
                            .padding(16)
                            .background(Color(UIColor.secondarySystemBackground))
                            .cornerRadius(16)
                        }
                        .buttonStyle(PlainButtonStyle())
                        
                        // Network Mode
                        HStack(spacing: 16) {
                            ZStack {
                                Circle()
                                    .fill(networkState.mode == .online ? Color.green.opacity(0.15) : Color.gray.opacity(0.15))
                                    .frame(width: 44, height: 44)
                                
                                Image(systemName: networkState.mode == .online ? "wifi" : "wifi.slash")
                                    .font(.system(size: 20))
                                    .foregroundColor(networkState.mode == .online ? .green : .gray)
                            }
                            
                            VStack(alignment: .leading, spacing: 4) {
                                Text(AppStrings.Settings.networkMode)
                                    .font(.subheadline)
                                    .fontWeight(.medium)

                                Text(networkState.mode == .online ? AppStrings.Settings.connected : AppStrings.Settings.offlineMode)
                                    .font(.caption)
                                    .foregroundColor(.secondary)
                            }
                            
                            Spacer()
                            
                            Toggle("", isOn: Binding(
                                get: { networkState.mode == .online },
                                set: { _ in networkState.toggleMode() }
                            ))
                            .labelsHidden()
                        }
                        .padding(16)
                        .background(Color(UIColor.secondarySystemBackground))
                        .cornerRadius(16)
                        
                        // Sync Progress (if syncing)
                        if networkState.isSyncing {
                            VStack(spacing: 12) {
                                HStack {
                                    Text(AppStrings.Settings.syncing)
                                        .font(.subheadline)
                                        .fontWeight(.medium)
                                    
                                    Spacer()
                                    
                                    Text("\(networkState.syncProgress)/\(networkState.syncTotal)")
                                        .font(.caption)
                                        .foregroundColor(.secondary)
                                }
                                
                                ProgressView(value: Double(networkState.syncProgress), total: Double(networkState.syncTotal))
                                    .progressViewStyle(.linear)
                                    .tint(.blue)
                            }
                            .padding(16)
                            .background(Color(UIColor.secondarySystemBackground))
                            .cornerRadius(16)
                        }
                    }
                    .padding(.horizontal)
                }
                
                // MARK: - Equipment Library Section (temporarily disabled)
                 VStack(alignment: .leading, spacing: 12) {
                     Text(AppStrings.Settings.equipmentLibrary)
                         .font(.headline)
                         .foregroundColor(.secondary)
                         .padding(.horizontal)
                
                     // ZP-2161 Phase 4b: hide the EasyPower download
                     // button. Infra (service + state + confirmation
                     // alert) is intentionally retained as a fallback
                     // until the SKM matcher is fully shipped. To
                     // restore: change ``false`` to ``true`` below.
                     if false {
                     Button(action: {
                         showingEqpLibraryConfirmation = true
                     }) {
                         VStack(spacing: 12) {
                             HStack(spacing: 16) {
                                 ZStack {
                                     Circle()
                                         .fill(Color.purple.opacity(0.15))
                                         .frame(width: 44, height: 44)
                
                                     Image(systemName: "square.and.arrow.down")
                                         .font(.system(size: 20))
                                         .foregroundColor(.purple)
                                 }
                
                                 VStack(alignment: .leading, spacing: 4) {
                                     Text(AppStrings.Settings.loadLatestEquipmentLibrary)
                                         .font(.subheadline)
                                         .fontWeight(.medium)
                
                                     if isDownloadingEqpLibrary {
                                         Text(eqpLibraryProgressLabel)
                                             .font(.caption)
                                             .foregroundColor(.blue)
                                     } else if let error = eqpLibraryError {
                                         Text(error)
                                             .font(.caption)
                                             .foregroundColor(.red)
                                     } else if !eqpLibraryStatus.isEmpty {
                                         Text(eqpLibraryStatus)
                                             .font(.caption)
                                             .foregroundColor(.green)
                                     } else {
                                         Text(eqpLibraryStatusText())
                                             .font(.caption)
                                             .foregroundColor(.secondary)
                                     }
                                 }
                
                                 Spacer()
                             }
                
                             if isDownloadingEqpLibrary {
                                 ProgressView(value: eqpLibraryProgress, total: 1.0)
                                     .progressViewStyle(.linear)
                                     .tint(.purple)
                             }
                         }
                         .padding(16)
                         .background(Color(UIColor.secondarySystemBackground))
                         .cornerRadius(16)
                     }
                     .buttonStyle(PlainButtonStyle())
                     .disabled(isDownloadingEqpLibrary || networkState.mode != .online)
                     .opacity(networkState.mode != .online ? 0.5 : 1.0)
                     .padding(.horizontal)
                
                     if networkState.mode != .online {
                         Text(AppStrings.Settings.goOnlineToDownload)
                             .font(.caption)
                             .foregroundColor(.orange)
                             .padding(.horizontal, 32)
                     }
                     } // end if false (hidden EasyPower button)

                     // ZP-2161 Phase 4b: SKM library download. Reuses
                     // the EasyPower button's visual vocabulary (purple
                     // ``square.and.arrow.down`` + "Load Latest
                     // Equipment Library") so existing users find it
                     // exactly where the old library used to live.
                     // Gated on the ``eng-lib`` company feature flag —
                     // the card stays visible-but-disabled when off so
                     // the capability is discoverable.
                     let engLibEnabled = AuthService.shared.hasFeature("eng-lib")
                     Button(action: {
                         guard engLibEnabled else { return }
                         showingSkmLibraryConfirmation = true
                     }) {
                         VStack(spacing: 12) {
                             HStack(spacing: 16) {
                                 ZStack {
                                     Circle()
                                         .fill(Color.purple.opacity(0.15))
                                         .frame(width: 44, height: 44)

                                     Image(systemName: "square.and.arrow.down")
                                         .font(.system(size: 20))
                                         .foregroundColor(.purple)
                                 }

                                 VStack(alignment: .leading, spacing: 4) {
                                     Text(AppStrings.Settings.loadLatestEquipmentLibrary)
                                         .font(.subheadline)
                                         .fontWeight(.medium)

                                     if isDownloadingSkmLibrary {
                                         Text(skmLibraryProgressLabel)
                                             .font(.caption)
                                             .foregroundColor(.blue)
                                     } else if let error = skmLibraryError {
                                         Text(error)
                                             .font(.caption)
                                             .foregroundColor(.red)
                                     } else if !skmLibraryStatus.isEmpty {
                                         Text(skmLibraryStatus)
                                             .font(.caption)
                                             .foregroundColor(.green)
                                     } else {
                                         Text(skmLibraryStatusText())
                                             .font(.caption)
                                             .foregroundColor(.secondary)
                                     }
                                 }

                                 Spacer()
                             }

                             if isDownloadingSkmLibrary {
                                 ProgressView(value: skmLibraryProgress, total: 1.0)
                                     .progressViewStyle(.linear)
                                     .tint(.purple)
                             }
                         }
                         .padding(16)
                         .background(Color(UIColor.secondarySystemBackground))
                         .cornerRadius(16)
                     }
                     .buttonStyle(PlainButtonStyle())
                     .disabled(!engLibEnabled || isDownloadingSkmLibrary || networkState.mode != .online)
                     .opacity((!engLibEnabled || networkState.mode != .online) ? 0.5 : 1.0)
                     .padding(.horizontal)

                     if !engLibEnabled {
                         Text(AppStrings.Engineering.engineeringLibraryDisabled)
                             .font(.caption)
                             .foregroundColor(.secondary)
                             .padding(.horizontal, 32)
                     } else if networkState.mode != .online {
                         Text(AppStrings.Settings.goOnlineToDownload)
                             .font(.caption)
                             .foregroundColor(.orange)
                             .padding(.horizontal, 32)
                     }
                 }

                // Diagnostics Section
                VStack(alignment: .leading, spacing: 12) {
                    Text(AppStrings.Settings.diagnostics)
                        .font(.headline)
                        .foregroundColor(.secondary)
                        .padding(.horizontal)

                    NavigationLink(destination: PhotoDiagnosticsView()) {
                        HStack(spacing: 16) {
                            ZStack {
                                Circle()
                                    .fill(Color.orange.opacity(0.15))
                                    .frame(width: 44, height: 44)

                                Image(systemName: "photo.badge.exclamationmark")
                                    .font(.system(size: 20))
                                    .foregroundColor(.orange)
                            }

                            VStack(alignment: .leading, spacing: 4) {
                                Text(AppStrings.Settings.photoStorageDiagnostics)
                                    .font(.subheadline)
                                    .fontWeight(.medium)

                                Text(AppStrings.Settings.checkPhotoStorageHealth)
                                    .font(.caption)
                                    .foregroundColor(.secondary)
                            }

                            Spacer()

                            Image(systemName: "chevron.right")
                                .font(.system(size: 12, weight: .semibold))
                                .foregroundColor(.accentColor)
                        }
                        .padding(16)
                        .background(Color(UIColor.secondarySystemBackground))
                        .cornerRadius(16)
                    }
                    .buttonStyle(PlainButtonStyle())
                    .padding(.horizontal)

                    // ZP-1723 milestone-1 dev entry point — lists every
                    // synced EG form instance and renders it natively.
                    // ZP-2336: gated on the ``eg-forms`` company feature flag.
                    // ZP-2404: temporarily hidden from Settings UI.
                    /*
                    if AuthService.shared.hasFeature("eg-forms") {
                        NavigationLink(destination: EGFormsDebugView()) {
                            HStack(spacing: 16) {
                                ZStack {
                                    Circle()
                                        .fill(Color.cyan.opacity(0.15))
                                        .frame(width: 44, height: 44)
                                    Image(systemName: "doc.text.fill")
                                        .font(.system(size: 20))
                                        .foregroundColor(.cyan)
                                }
                                VStack(alignment: .leading, spacing: 4) {
                                    Text("EG Forms (beta)")
                                        .font(.subheadline)
                                        .fontWeight(.medium)
                                    Text("Preview synced EG form instances")
                                        .font(.caption)
                                        .foregroundColor(.secondary)
                                }
                                Spacer()
                                Image(systemName: "chevron.right")
                                    .font(.system(size: 12, weight: .semibold))
                                    .foregroundColor(.accentColor)
                            }
                            .padding(16)
                            .background(Color(UIColor.secondarySystemBackground))
                            .cornerRadius(16)
                        }
                        .buttonStyle(PlainButtonStyle())
                        .padding(.horizontal)
                    }
                    */
                }

                // Storage Section
                VStack(alignment: .leading, spacing: 12) {
                    Text(AppStrings.Settings.storage)
                        .font(.headline)
                        .foregroundColor(.secondary)
                        .padding(.horizontal)

                    Button(action: {
                        showingClearCacheConfirmation = true
                    }) {
                        HStack(spacing: 16) {
                            ZStack {
                                Circle()
                                    .fill(Color.red.opacity(0.15))
                                    .frame(width: 44, height: 44)

                                Image(systemName: "trash.circle")
                                    .font(.system(size: 20))
                                    .foregroundColor(.red)
                            }

                            VStack(alignment: .leading, spacing: 4) {
                                Text(AppStrings.Settings.clearImageCache)
                                    .font(.subheadline)
                                    .fontWeight(.medium)

                                if showCacheClearedMessage {
                                    Text(AppStrings.Settings.cacheClearedSuccessfully)
                                        .font(.caption)
                                        .foregroundColor(.green)
                                } else {
                                    Text(AppStrings.Settings.cacheSize(cacheSizeText))
                                        .font(.caption)
                                        .foregroundColor(.secondary)
                                }
                            }

                            Spacer()

                            if isClearingCache {
                                ProgressView()
                                    .progressViewStyle(CircularProgressViewStyle())
                            }
                        }
                        .padding(16)
                        .background(Color(UIColor.secondarySystemBackground))
                        .cornerRadius(16)
                    }
                    .buttonStyle(PlainButtonStyle())
                    .disabled(isClearingCache)
                    .padding(.horizontal)
                }

                // About Section
                VStack(alignment: .leading, spacing: 12) {
                    Text(AppStrings.Settings.about)
                        .font(.headline)
                        .foregroundColor(.secondary)
                        .padding(.horizontal)

                    VStack(spacing: 0) {
                        HStack {
                            Text(AppStrings.Common.version)
                                .font(.subheadline)
                            Spacer()
                            Text(getAppVersion())
                                .font(.subheadline)
                                .foregroundColor(.secondary)
                        }
                        .padding(16)

                        if let sldVersion = SldViewerAssetManager.shared.getStoredVersion() {
                            Divider().padding(.leading, 16)
                            HStack {
                                Text(AppStrings.Settings.sldViewerVersion)
                                    .font(.subheadline)
                                Spacer()
                                Text(sldVersion)
                                    .font(.subheadline)
                                    .foregroundColor(.secondary)
                            }
                            .padding(16)
                        }
                    }
                    .background(Color(UIColor.secondarySystemBackground))
                    .cornerRadius(16)
                    .padding(.horizontal)
                }
                
                Spacer(minLength: 20)
                
                // ZP-1847 — a pending queue alone never blocks the user; only an
                // active sync, an in-flight logout, or being offline does. The
                // queue persists per-user on device, so signing out (and back
                // in) does not lose work.
                // ZP-2173 — but legacy queue items (`userId == nil`) DO block
                // logout because they depend on the live SwiftData entity tables;
                // logging out clears them, turning every legacy row into a
                // permanent "Entity no longer exists" failure on next sync.
                let lifecycleBusy = authService.isLoggingOut || authService.isLoading
                let logoutDisabled = lifecycleBusy
                    || networkState.isSyncing
                    || !networkState.isOnline
                    || networkState.hasLegacySyncItems

                // Logout Button
                VStack(spacing: 8) {
                    Button(action: {
                        showingLogoutConfirmation = true
                    }) {
                        HStack {
                            if lifecycleBusy {
                                ProgressView()
                                    .progressViewStyle(CircularProgressViewStyle(tint: .white))
                                    .scaleEffect(0.8)
                            }
                            Text(lifecycleBusy ? AppStrings.Settings.loggingOut : AppStrings.Common.logout)
                                .font(.subheadline)
                                .fontWeight(.medium)
                                .foregroundColor(.white)
                        }
                        .frame(maxWidth: .infinity)
                        .padding()
                        .background(logoutDisabled ? Color.gray : Color.red)
                        .cornerRadius(16)
                    }
                    .disabled(logoutDisabled)

                    if !networkState.isOnline {
                        Text(AppStrings.Settings.logoutRequiresOnline)
                            .font(.caption)
                            .foregroundColor(.orange)
                            .multilineTextAlignment(.center)
                    } else if networkState.isSyncing {
                        Text(AppStrings.Settings.logoutBlockedSyncing)
                            .font(.caption)
                            .foregroundColor(.orange)
                            .multilineTextAlignment(.center)
                    } else if networkState.hasLegacySyncItems {
                        Text(AppStrings.Diagnostics.legacyItemsBlockingAction)
                            .font(.caption)
                            .foregroundColor(.orange)
                            .multilineTextAlignment(.center)
                    } else if networkState.syncQueueCount > 0 {
                        Text(AppStrings.Settings.syncPendingNote(count: networkState.syncQueueCount))
                            .font(.caption)
                            .foregroundColor(.secondary)
                            .multilineTextAlignment(.center)
                    }
                }
                .padding(.horizontal)
            }
            .padding(.vertical, 20)
        }
        .background(Color(UIColor.systemBackground))
        .navigationTitle(AppStrings.Settings.title)
        .navigationBarTitleDisplayMode(.large)
        .alert(
            AppStrings.Settings.logoutConfirmation,
            isPresented: $showingLogoutConfirmation
        ) {
            Button(AppStrings.Common.logout, role: .destructive) {
                performLogout()
            }
            Button(AppStrings.Common.cancel, role: .cancel) {}
        } message: {
            Text(AppStrings.Settings.logoutMessage)
        }
        .alert(AppStrings.Settings.logoutError, isPresented: $showingLogoutError) {
            Button(AppStrings.Common.ok, role: .cancel) {}
        } message: {
            Text(logoutErrorMessage)
        }
        .onChange(of: sessionRecordingEnabled) { _, newValue in
            Task {
                await DevRevService.shared.setRecordingEnabled(newValue)
            }
        }
        .alert(AppStrings.Settings.disableBiometric(type: biometricService.biometricType.displayName), isPresented: $showingBiometricDisableAlert) {
            Button(AppStrings.Common.disable, role: .destructive) {
                biometricService.disableBiometricAuth()
            }
            Button(AppStrings.Common.cancel, role: .cancel) {}
        } message: {
            Text(AppStrings.Settings.enterPasswordToSignIn)
        }
        .sheet(isPresented: $showingBiometricEnableSheet) {
            NavigationView {
                VStack(spacing: 24) {
                    VStack(spacing: 8) {
                        Image(systemName: biometricService.biometricType.iconName)
                            .font(.system(size: 60))
                            .foregroundColor(.blue)

                        Text(AppStrings.Settings.enableBiometric(type: biometricService.biometricType.displayName))
                            .font(.title2)
                            .fontWeight(.semibold)

                        Text(AppStrings.Settings.enterPasswordForBiometric(type: biometricService.biometricType.displayName))
                            .font(.subheadline)
                            .foregroundColor(.secondary)
                            .multilineTextAlignment(.center)
                            .padding(.horizontal)
                    }
                    .padding(.top, 20)

                    VStack(alignment: .leading, spacing: 8) {
                        Text(AppStrings.Common.password)
                            .font(.subheadline)
                            .foregroundColor(.secondary)

                        HStack {
                            if showPassword {
                                TextField(AppStrings.Settings.enterYourPassword, text: $passwordForBiometric)
                                    .textContentType(.password)
                                    .autocapitalization(.none)
                                    .disableAutocorrection(true)
                            } else {
                                SecureField(AppStrings.Settings.enterYourPassword, text: $passwordForBiometric)
                                    .textContentType(.password)
                            }

                            Button(action: {
                                showPassword.toggle()
                            }) {
                                Image(systemName: showPassword ? "eye.slash.fill" : "eye.fill")
                                    .foregroundColor(.secondary)
                            }
                        }
                        .padding(12)
                        .background(Color(UIColor.secondarySystemBackground))
                        .cornerRadius(8)
                    }
                    .padding(.horizontal)

                    if let error = biometricEnableError {
                        Text(error)
                            .font(.caption)
                            .foregroundColor(.red)
                            .padding(.horizontal)
                    }

                    Button(action: setupBiometricWithPassword) {
                        HStack {
                            if isEnablingBiometric {
                                ProgressView()
                                    .progressViewStyle(CircularProgressViewStyle(tint: .white))
                                    .scaleEffect(0.8)
                            }
                            Text(isEnablingBiometric ? AppStrings.Common.verifying : AppStrings.Settings.enableBiometric(type: biometricService.biometricType.displayName))
                        }
                        .frame(maxWidth: .infinity)
                        .padding()
                        .background(passwordForBiometric.isEmpty || isEnablingBiometric ? Color.blue.opacity(0.6) : Color.blue)
                        .foregroundColor(.white)
                        .cornerRadius(12)
                    }
                    .disabled(passwordForBiometric.isEmpty || isEnablingBiometric)
                    .padding(.horizontal)

                    Spacer()
                }
                .navigationBarTitleDisplayMode(.inline)
                .toolbar {
                    ToolbarItem(placement: .cancellationAction) {
                        Button(AppStrings.Common.cancel) {
                            showingBiometricEnableSheet = false
                            passwordForBiometric = ""
                            biometricEnableError = nil
                            showPassword = false
                        }
                    }
                }
            }
            .presentationDetents([.medium])
        }
        .alert(
            AppStrings.Settings.clearImageCache,
            isPresented: $showingClearCacheConfirmation
        ) {
            Button(AppStrings.Common.clear, role: .destructive) {
                Task { await clearImageCache() }
            }
            Button(AppStrings.Common.cancel, role: .cancel) {}
        } message: {
            Text(AppStrings.Settings.clearImageCacheMessage)
        }
        .alert(
            AppStrings.Settings.downloadEquipmentLibraryConfirmation,
            isPresented: $showingEqpLibraryConfirmation
        ) {
            Button(AppStrings.Settings.download) {
                Task { await downloadEquipmentLibrary() }
            }
            Button(AppStrings.Common.cancel, role: .cancel) {}
        } message: {
            Text(AppStrings.Settings.downloadEquipmentLibraryMessage)
        }
        .alert(
            AppStrings.Engineering.loadDeviceLibraryConfirmation,
            isPresented: $showingSkmLibraryConfirmation
        ) {
            Button(AppStrings.Settings.download) {
                Task { await downloadSkmLibrary() }
            }
            Button(AppStrings.Common.cancel, role: .cancel) {}
        } message: {
            Text(AppStrings.Engineering.loadDeviceLibraryMessage)
        }
        .alert(
            AppStrings.Settings.language,
            isPresented: $showingLanguageConfirmation
        ) {
            Button(AppStrings.Common.ok) {
                applyLanguageChange()
            }
            Button(AppStrings.Common.cancel, role: .cancel) {
                pendingLanguage = nil
            }
        } message: {
            if let language = pendingLanguage {
                Text(AppStrings.Settings.switchLanguageMessage(language.displayName))
            }
        }
        .onAppear {
            biometricService.checkBiometricAvailability()
            updateCacheSizeText()
        }
    }

    private func applyLanguageChange() {
        if let language = pendingLanguage {
            languageManager.setLanguage(language)
            // ZP-2061: remember this as the user's preferred language so the
            // office-driven auto-switch on site selection can restore it.
            SiteLanguageController.shared.onUserManuallyPickedLanguage(language)
            pendingLanguage = nil
            Task {
                await refreshFromServer()
            }
        }
    }

    private func refreshFromServer() async {
        do {
            try await sldService.refreshSLD(modelContext: modelContext)
        } catch {
            guard !AuthError.isAuthError(error) else { return }
        }
    }

    private func performLogout() {
        Task {
            do {
                // ZP-1847: gated logout. Refuses if offline / syncing /
                // already-logging-out, and aborts cleanly if the server
                // logout call fails — user stays signed in so they can
                // retry without losing pending work.
                try await authService.requestLogout()

                // NOTE: We intentionally keep biometric auth enabled after logout
                // so users can quickly sign back in with FaceID

                // Clear app state
                appState.loggedIn = false
                appState.userId = UUID() // Reset to default

                // The ContentView will handle navigation back to login
                // based on authService.isAuthenticated becoming false
            } catch {
                logoutErrorMessage = error.localizedDescription
                showingLogoutError = true
            }
        }
    }


    private func enableBiometric() {
        // We need the user's current email to set up biometric
        guard authService.currentUser?.email != nil else {
            AppLogger.log(.notice, "No user email available for biometric setup", category: .ui)
            return
        }

        // Show password entry sheet to re-authenticate
        showingBiometricEnableSheet = true
    }

    private func setupBiometricWithPassword() {
        guard let email = authService.currentUser?.email else { return }

        isEnablingBiometric = true
        biometricEnableError = nil

        Task {
            do {
                // Validate password by attempting login
                let result = try await authService.login(email: email, password: passwordForBiometric, subdomain: nil)

                if result.success {
                    // Password valid, setup biometric
                    try await biometricService.setupBiometricAuth(email: email, password: passwordForBiometric)
                    showingBiometricEnableSheet = false
                    passwordForBiometric = ""
                    showPassword = false
                }
            } catch {
                biometricEnableError = AppStrings.Settings.invalidPassword
            }
            isEnablingBiometric = false
        }
    }

    private func updateCacheSizeText() {
        let photoImageCacheSize = PhotoImageCache.shared.getDiskCacheSize()
        let urlCacheSize = ImageCacheManager.shared.getCacheStats().diskUsage
        let totalBytes = photoImageCacheSize + urlCacheSize
        cacheSizeText = formatByteCount(totalBytes)
    }

    private func formatByteCount(_ bytes: Int) -> String {
        if bytes < 1024 {
            return "\(bytes) B"
        } else if bytes < 1024 * 1024 {
            return String(format: "%.1f KB", Double(bytes) / 1024.0)
        } else {
            return String(format: "%.1f MB", Double(bytes) / (1024.0 * 1024.0))
        }
    }

    private func clearImageCache() async {
        isClearingCache = true
        // Run file I/O off the main thread so the spinner renders and UI isn't blocked
        await Task.detached(priority: .userInitiated) {
            PhotoImageCache.shared.clearAllCaches()
            ImageCacheManager.shared.clearDiskCache()
        }.value
        // Set to "0 B" directly — URLCache.currentDiskUsage reports stale values
        // immediately after removeAllCachedResponses()
        cacheSizeText = formatByteCount(0)
        isClearingCache = false
        showCacheClearedMessage = true
        try? await Task.sleep(nanoseconds: 2_000_000_000)
        showCacheClearedMessage = false
    }

    private func downloadEquipmentLibrary() async {
        isDownloadingEqpLibrary = true
        eqpLibraryError = nil
        eqpLibraryStatus = ""
        eqpLibraryProgress = 0
        eqpLibraryProgressLabel = "Starting..."

        do {
            guard let container = AppStateManager.shared.modelContainer else {
                throw NSError(domain: "EqpLib", code: -1,
                              userInfo: [NSLocalizedDescriptionKey: "ModelContainer not available"])
            }
            let result = try await EqpLibrarySyncService.downloadAndCache(
                modelContainer: container,
                onProgress: { label, progress in
                    Task { @MainActor in
                        eqpLibraryProgressLabel = label
                        eqpLibraryProgress = progress
                    }
                }
            )
            eqpLibraryStatus = "\(result.itemCount) items, \(result.styleCount) styles, \(result.sensorCount) sensors"
        } catch {
            eqpLibraryError = "Download failed: \(error.localizedDescription)"
        }

        isDownloadingEqpLibrary = false
    }

    private func eqpLibraryStatusText() -> String {
        if let date = EqpLibrarySyncService.lastDownloadDate {
            let formatter = RelativeDateTimeFormatter()
            formatter.unitsStyle = .abbreviated
            return AppStrings.Settings.lastUpdated(formatter.localizedString(for: date, relativeTo: Date()))
        }
        return AppStrings.Settings.notYetDownloaded
    }

    private func downloadSkmLibrary() async {
        isDownloadingSkmLibrary = true
        skmLibraryError = nil
        skmLibraryStatus = ""
        skmLibraryProgress = 0
        skmLibraryProgressLabel = AppStrings.Engineering.starting

        do {
            guard let container = AppStateManager.shared.modelContainer else {
                throw NSError(domain: "SkmLib", code: -1,
                              userInfo: [NSLocalizedDescriptionKey: "ModelContainer not available"])
            }
            let result = try await SkmLibrarySyncService.downloadAndCache(
                modelContainer: container,
                onProgress: { label, progress in
                    Task { @MainActor in
                        skmLibraryProgressLabel = label
                        skmLibraryProgress = progress
                    }
                }
            )
            skmLibraryStatus = "\(result.frames.formatted()) frames, \(result.sensors.formatted()) sensors, \(result.tripUnits.formatted()) trip units, \(result.segments.formatted()) segments, \(result.kvaEntries.formatted()) kVA entries, \(result.cableEntries.formatted()) cable / busway entries"
        } catch {
            skmLibraryError = AppStrings.Engineering.downloadFailed(error.localizedDescription)
        }

        isDownloadingSkmLibrary = false
    }

    private func skmLibraryStatusText() -> String {
        if let date = SkmLibrarySyncService.lastDownloadDate {
            let formatter = RelativeDateTimeFormatter()
            formatter.unitsStyle = .abbreviated
            return AppStrings.Settings.lastUpdated(formatter.localizedString(for: date, relativeTo: Date()))
        }
        return AppStrings.Settings.notYetDownloaded
    }

    private func getAppVersion() -> String {
        let version = Bundle.main.object(forInfoDictionaryKey: "CFBundleShortVersionString") as? String ?? "Unknown"
        let build = Bundle.main.object(forInfoDictionaryKey: "CFBundleVersion") as? String ?? "Unknown"
        return "\(version) (\(build))"
    }
}
