# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is an iOS application built with SwiftUI and SwiftData. The app appears to be a field inspection/reporting system with offline capabilities and synchronization features.

## Architecture

### Core Components

**Models & Data Layer:**
- Uses SwiftData for persistence with models in `SwiftDataTutorial/Core/Models/Entities/`
- Key entities: SLDV2, NodeV2, EdgeV2, Photo, IRSession, Issue, Quote, UserTaskForm
- DTOs in `SwiftDataTutorial/Core/Models/DTOs/` for API communication
- Sync queue system for offline/online data synchronization

**Services & Singletons:**
- `APIClient`: Central API communication handler with comprehensive error handling
- `NetworkState`: Manages online/offline mode and sync operations
- `SLDSyncService`: Handles data synchronization between local and remote
- `AuthService`: Authentication and user session management
- `AppStateManager`: Global app state coordination
- Services use singleton pattern (`.shared` instances)

**Key Architectural Patterns:**
- Heavy use of `@Published` properties and `ObservableObject` for reactive UI
- Offline-first architecture with sync queue (`SyncQueueItem`, `SyncLog`)
- Service-oriented architecture with dedicated services for different domains
- Photo upload service with retry mechanisms

## Development Commands

### Building & Running
```bash
# Build the project
xcodebuild -project SwiftDataTutorial.xcodeproj -scheme SwiftDataTutorial -configuration Debug build

# Clean build folder
xcodebuild -project SwiftDataTutorial.xcodeproj -scheme SwiftDataTutorial clean

# Run on simulator
xcodebuild -project SwiftDataTutorial.xcodeproj -scheme SwiftDataTutorial -destination 'platform=iOS Simulator,name=iPhone 15'

# Build for release
xcodebuild -project SwiftDataTutorial.xcodeproj -scheme SwiftDataTutorial -configuration Release
```

### Testing
```bash
# Run unit tests
xcodebuild test -project SwiftDataTutorial.xcodeproj -scheme SwiftDataTutorial -destination 'platform=iOS Simulator,name=iPhone 15'
```

## Project Structure

```
project-z-mobile/
├── SwiftDataTutorial/           # Main app code
│   ├── App.swift               # App entry point with SwiftData configuration
│   ├── ContentView.swift       # Root view
│   ├── Core/
│   │   ├── Models/
│   │   │   ├── Entities/      # SwiftData models
│   │   │   ├── DTOs/          # API data transfer objects
│   │   │   └── Sync/          # Sync-related models
│   │   ├── Singletons/        # Shared service instances
│   │   └── Services/          # Business logic services
│   ├── Views/                 # UI components
│   └── Bridges/               # Likely bridging code for integrations
└── SwiftDataTutorial.xcodeproj # Xcode project file
```

## Important Implementation Notes

- The app uses a network mode system (online/offline) managed by `NetworkState`
- Sync operations are queued and executed based on network availability
- Photo uploads are handled separately with retry logic
- The app maintains sync logs for debugging and auditing
- Authentication state is managed globally through `AuthService`
- Multiple model containers are configured in the app for different entity types