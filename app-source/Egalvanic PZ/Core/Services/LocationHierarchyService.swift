//
//  LocationHierarchyService.swift
//  Egalvanic PZ
//
//  Service for managing Building, Floor, and Room hierarchy
//

import Foundation
import SwiftData

/// Service responsible for managing the location hierarchy (Building → Floor → Room)
@MainActor
class LocationHierarchyService {

    // MARK: - Building Operations

    /// Create a new building
    static func createBuilding(
        name: String,
        sld: SLDV2,
        accessNotes: String? = nil,
        networkState: NetworkState,
        modelContext: ModelContext
    ) async throws -> Building {
        let building = Building(
            id: UUID(),
            name: name,
            sld: sld,
            is_deleted: false,
            access_notes: accessNotes
        )

        // Insert locally
        modelContext.insert(building)
        try modelContext.save()

        AppLogger.log(.info, "Created building locally: \(name)", category: .location)

        // Sync to server if online
        if networkState.canDirectSync {
            do {
                _ = try await APIClient.shared.createBuilding(building: building)
                AppLogger.log(.info, "Synced building to server: \(name)", category: .location)
            } catch {
                AppLogger.log(.error, "Failed to sync building to server: \(error)", category: .location)
                // Queue for later sync
                let op = SyncOp(
                    target: .building,
                    operation: .create,
                    building: building
                )
                networkState.enqueue(op)
            }
        } else {
            // Queue for later sync
            let op = SyncOp(
                target: .building,
                operation: .create,
                building: building
            )
            networkState.enqueue(op)
        }

        return building
    }

    /// Update an existing building
    static func updateBuilding(
        _ building: Building,
        name: String,
        accessNotes: String? = nil,
        networkState: NetworkState,
        modelContext: ModelContext
    ) async throws {
        // Update locally
        building.name = name
        building.access_notes = accessNotes
        try modelContext.save()

        AppLogger.log(.info, "Updated building locally: \(name)", category: .location)

        // Sync to server if online
        if networkState.canDirectSync {
            do {
                _ = try await APIClient.shared.updateBuilding(building)
                AppLogger.log(.info, "Synced building update to server: \(name)", category: .location)
            } catch {
                AppLogger.log(.error, "Failed to sync building update to server: \(error)", category: .location)
                // Queue for later sync
                let op = SyncOp(
                    target: .building,
                    operation: .update,
                    building: building
                )
                networkState.enqueue(op)
            }
        } else {
            // Queue for later sync
            let op = SyncOp(
                target: .building,
                operation: .update,
                building: building
            )
            networkState.enqueue(op)
        }
    }

    /// Delete a building (soft delete)
    static func deleteBuilding(
        _ building: Building,
        networkState: NetworkState,
        modelContext: ModelContext
    ) async throws {
        // Soft delete locally
        building.is_deleted = true
        try modelContext.save()

        AppLogger.log(.info, "Soft deleted building locally: \(building.name)", category: .location)

        // Sync to server if online
        if networkState.canDirectSync {
            do {
                _ = try await APIClient.shared.updateBuilding(building)
                AppLogger.log(.info, "Synced building deletion to server", category: .location)
            } catch {
                AppLogger.log(.error, "Failed to sync building deletion to server: \(error)", category: .location)
                // Queue for later sync
                let op = SyncOp(
                    target: .building,
                    operation: .update,
                    building: building
                )
                networkState.enqueue(op)
            }
        } else {
            // Queue for later sync
            let op = SyncOp(
                target: .building,
                operation: .update,
                building: building
            )
            networkState.enqueue(op)
        }
    }

    // MARK: - Floor Operations

    /// Create a new floor
    static func createFloor(
        name: String,
        building: Building,
        accessNotes: String? = nil,
        networkState: NetworkState,
        modelContext: ModelContext
    ) async throws -> Floor {
        let floor = Floor(
            id: UUID(),
            name: name,
            building: building,
            is_deleted: false,
            access_notes: accessNotes
        )

        // Insert locally
        modelContext.insert(floor)
        try modelContext.save()

        AppLogger.log(.info, "Created floor locally: \(name)", category: .location)

        // Sync to server if online
        if networkState.canDirectSync {
            do {
                _ = try await APIClient.shared.createFloor(floor: floor)
                AppLogger.log(.info, "Synced floor to server: \(name)", category: .location)
            } catch {
                AppLogger.log(.error, "Failed to sync floor to server: \(error)", category: .location)
                // Queue for later sync
                let op = SyncOp(
                    target: .floor,
                    operation: .create,
                    floor: floor
                )
                networkState.enqueue(op)
            }
        } else {
            // Queue for later sync
            let op = SyncOp(
                target: .floor,
                operation: .create,
                floor: floor
            )
            networkState.enqueue(op)
        }

        return floor
    }

    /// Update an existing floor
    static func updateFloor(
        _ floor: Floor,
        name: String,
        building: Building?,
        accessNotes: String? = nil,
        networkState: NetworkState,
        modelContext: ModelContext
    ) async throws {
        // Update locally
        floor.name = name
        if let building = building {
            floor.building = building
        }
        floor.access_notes = accessNotes
        try modelContext.save()

        AppLogger.log(.info, "Updated floor locally: \(name)", category: .location)

        // Sync to server if online
        if networkState.canDirectSync {
            do {
                _ = try await APIClient.shared.updateFloor(floor)
                AppLogger.log(.info, "Synced floor update to server: \(name)", category: .location)
            } catch {
                AppLogger.log(.error, "Failed to sync floor update to server: \(error)", category: .location)
                // Queue for later sync
                let op = SyncOp(
                    target: .floor,
                    operation: .update,
                    floor: floor
                )
                networkState.enqueue(op)
            }
        } else {
            // Queue for later sync
            let op = SyncOp(
                target: .floor,
                operation: .update,
                floor: floor
            )
            networkState.enqueue(op)
        }
    }

    /// Delete a floor (soft delete)
    static func deleteFloor(
        _ floor: Floor,
        networkState: NetworkState,
        modelContext: ModelContext
    ) async throws {
        // Soft delete locally
        floor.is_deleted = true
        try modelContext.save()

        AppLogger.log(.info, "Soft deleted floor locally: \(floor.name)", category: .location)

        // Sync to server if online
        if networkState.canDirectSync {
            do {
                _ = try await APIClient.shared.updateFloor(floor)
                AppLogger.log(.info, "Synced floor deletion to server", category: .location)
            } catch {
                AppLogger.log(.error, "Failed to sync floor deletion to server: \(error)", category: .location)
                // Queue for later sync
                let op = SyncOp(
                    target: .floor,
                    operation: .update,
                    floor: floor
                )
                networkState.enqueue(op)
            }
        } else {
            // Queue for later sync
            let op = SyncOp(
                target: .floor,
                operation: .update,
                floor: floor
            )
            networkState.enqueue(op)
        }
    }

    // MARK: - Room Operations

    /// Create a new room
    static func createRoom(
        name: String,
        floor: Floor,
        accessNotes: String? = nil,
        networkState: NetworkState,
        modelContext: ModelContext
    ) async throws -> Room {
        let room = Room(
            id: UUID(),
            name: name,
            floor: floor,
            is_deleted: false,
            access_notes: accessNotes
        )

        // Insert locally
        modelContext.insert(room)
        try modelContext.save()

        AppLogger.log(.info, "Created room locally: \(name)", category: .location)

        // Sync to server if online
        if networkState.canDirectSync {
            do {
                _ = try await APIClient.shared.createRoom(room: room)
                AppLogger.log(.info, "Synced room to server: \(name)", category: .location)
            } catch {
                AppLogger.log(.error, "Failed to sync room to server: \(error)", category: .location)
                // Queue for later sync
                let op = SyncOp(
                    target: .room,
                    operation: .create,
                    room: room
                )
                networkState.enqueue(op)
            }
        } else {
            // Queue for later sync
            let op = SyncOp(
                target: .room,
                operation: .create,
                room: room
            )
            networkState.enqueue(op)
        }

        return room
    }

    /// Update an existing room
    static func updateRoom(
        _ room: Room,
        name: String,
        floor: Floor?,
        accessNotes: String? = nil,
        networkState: NetworkState,
        modelContext: ModelContext
    ) async throws {
        // Update locally
        room.name = name
        if let floor = floor {
            room.floor = floor
        }
        room.access_notes = accessNotes
        try modelContext.save()

        AppLogger.log(.info, "Updated room locally: \(name)", category: .location)

        // Sync to server if online
        if networkState.canDirectSync {
            do {
                _ = try await APIClient.shared.updateRoom(room)
                AppLogger.log(.info, "Synced room update to server: \(name)", category: .location)
            } catch {
                AppLogger.log(.error, "Failed to sync room update to server: \(error)", category: .location)
                // Queue for later sync
                let op = SyncOp(
                    target: .room,
                    operation: .update,
                    room: room
                )
                networkState.enqueue(op)
            }
        } else {
            // Queue for later sync
            let op = SyncOp(
                target: .room,
                operation: .update,
                room: room
            )
            networkState.enqueue(op)
        }
    }

    /// Delete a room (soft delete)
    static func deleteRoom(
        _ room: Room,
        networkState: NetworkState,
        modelContext: ModelContext
    ) async throws {
        // Soft delete locally
        room.is_deleted = true
        try modelContext.save()

        AppLogger.log(.info, "Soft deleted room locally: \(room.name)", category: .location)

        // Sync to server if online
        if networkState.canDirectSync {
            do {
                _ = try await APIClient.shared.updateRoom(room)
                AppLogger.log(.info, "Synced room deletion to server", category: .location)
            } catch {
                AppLogger.log(.error, "Failed to sync room deletion to server: \(error)", category: .location)
                // Queue for later sync
                let op = SyncOp(
                    target: .room,
                    operation: .update,
                    room: room
                )
                networkState.enqueue(op)
            }
        } else {
            // Queue for later sync
            let op = SyncOp(
                target: .room,
                operation: .update,
                room: room
            )
            networkState.enqueue(op)
        }
    }

    // MARK: - Query Operations

    /// Get all buildings for an SLD
    static func getBuildingsForSLD(
        _ sld: SLDV2,
        modelContext: ModelContext
    ) throws -> [Building] {
        // Use relationship property directly - much faster than fetching all and filtering
        return sld.buildings
            .filter { !$0.is_deleted }
            .sorted { $0.name < $1.name }
    }

    /// Get all floors for a building
    static func getFloorsForBuilding(
        _ building: Building,
        modelContext: ModelContext
    ) throws -> [Floor] {
        // Use relationship property directly - much faster than fetching all and filtering
        return building.floors
            .filter { !$0.is_deleted }
            .sorted { $0.name < $1.name }
    }

    /// Get all rooms for a floor
    static func getRoomsForFloor(
        _ floor: Floor,
        modelContext: ModelContext
    ) throws -> [Room] {
        // Use relationship property directly - much faster than fetching all and filtering
        return floor.rooms
            .filter { !$0.is_deleted }
            .sorted { $0.name < $1.name }
    }

    /// Get all nodes for a room
    static func getNodesForRoom(
        _ room: Room,
        modelContext: ModelContext
    ) throws -> [NodeV2] {
        // Use relationship property directly - much faster than fetching all and filtering
        return room.nodes
            .filter { !$0.is_deleted }
            .sorted { $0.label < $1.label }
    }

    /// Get all nodes without a room assignment (for "No Location" virtual room)
    static func getNodesWithoutRoom(
        _ sld: SLDV2,
        modelContext: ModelContext
    ) throws -> [NodeV2] {
        let sldId = sld.id
        let descriptor = FetchDescriptor<NodeV2>(
            predicate: #Predicate<NodeV2> { node in
                node.sld?.id == sldId &&
                node.room == nil &&
                !node.is_deleted
            },
            sortBy: [SortDescriptor(\NodeV2.label)]
        )
        return try modelContext.fetch(descriptor)
    }
}
