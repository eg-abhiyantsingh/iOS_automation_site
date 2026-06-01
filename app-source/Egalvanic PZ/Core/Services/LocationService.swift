//
//  LocationService.swift
//  Egalvanic PZ
//
//  Created by Claude on 10/12/25.
//

import Foundation
import CoreLocation
import SwiftUI

/// Model representing structured address information from location services
struct AddressComponents {
    let streetNumber: String?
    let streetName: String?
    let city: String?
    let stateProvince: String?
    let postalCode: String?
    let countryCode: String?
    let formattedAddress: String?
    let latitude: Double?
    let longitude: Double?

    var addressLine1: String? {
        if let number = streetNumber, let street = streetName {
            return "\(number) \(street)"
        } else if let street = streetName {
            return street
        }
        return nil
    }
}

/// Service for handling location services and reverse geocoding
@MainActor
class LocationService: NSObject, ObservableObject {
    @Published var authorizationStatus: CLAuthorizationStatus = .notDetermined
    @Published var currentLocation: CLLocation?
    @Published var lastError: Error?
    @Published var isLoading = false

    private let locationManager = CLLocationManager()
    private let geocoder = CLGeocoder()

    override init() {
        super.init()
        locationManager.delegate = self
        locationManager.desiredAccuracy = kCLLocationAccuracyBest
        authorizationStatus = locationManager.authorizationStatus
    }

    /// Request location permissions
    func requestPermission() {
        locationManager.requestWhenInUseAuthorization()
    }

    /// Get current location
    func getCurrentLocation() async throws -> CLLocation {
        isLoading = true
        defer { isLoading = false }

        // Check authorization
        guard authorizationStatus == .authorizedWhenInUse || authorizationStatus == .authorizedAlways else {
            throw LocationError.notAuthorized
        }

        // Clear stale location so we wait for a fresh fix
        currentLocation = nil

        // Request location
        return try await withCheckedThrowingContinuation { continuation in
            locationManager.requestLocation()

            // Single task that polls for location with a timeout
            Task {
                var hasResumed = false
                for _ in 0..<50 { // 50 x 0.2s = 10 seconds max
                    if let location = self.currentLocation {
                        if !hasResumed {
                            hasResumed = true
                            continuation.resume(returning: location)
                        }
                        return
                    }
                    try? await Task.sleep(nanoseconds: 200_000_000) // 0.2 seconds
                }
                if !hasResumed {
                    continuation.resume(throwing: LocationError.timeout)
                }
            }
        }
    }

    /// Reverse geocode a location to get address components
    func reverseGeocode(location: CLLocation) async throws -> AddressComponents {
        isLoading = true
        defer { isLoading = false }

        let placemarks = try await geocoder.reverseGeocodeLocation(location)

        guard let placemark = placemarks.first else {
            throw LocationError.noPlacemark
        }

        return parseAddressComponents(from: placemark)
    }

    /// Get current location and reverse geocode it
    func getCurrentAddressComponents() async throws -> AddressComponents {
        let location = try await getCurrentLocation()
        return try await reverseGeocode(location: location)
    }

    /// Forward geocode an address string to get location
    func geocodeAddress(_ address: String) async throws -> (CLLocation, AddressComponents) {
        isLoading = true
        defer { isLoading = false }

        let placemarks = try await geocoder.geocodeAddressString(address)

        guard let placemark = placemarks.first,
              let location = placemark.location else {
            throw LocationError.noPlacemark
        }

        let components = parseAddressComponents(from: placemark)
        return (location, components)
    }

    // MARK: - Private Helpers

    private func parseAddressComponents(from placemark: CLPlacemark) -> AddressComponents {
        // Build formatted address
        var addressParts: [String] = []

        if let streetNumber = placemark.subThoroughfare {
            addressParts.append(streetNumber)
        }
        if let streetName = placemark.thoroughfare {
            addressParts.append(streetName)
        }

        let line1 = addressParts.joined(separator: " ")
        var formattedParts: [String] = []

        if !line1.isEmpty {
            formattedParts.append(line1)
        }

        if let city = placemark.locality {
            formattedParts.append(city)
        }

        if let state = placemark.administrativeArea {
            if let postalCode = placemark.postalCode {
                formattedParts.append("\(state) \(postalCode)")
            } else {
                formattedParts.append(state)
            }
        } else if let postalCode = placemark.postalCode {
            formattedParts.append(postalCode)
        }

        if let country = placemark.country {
            formattedParts.append(country)
        }

        let formatted = formattedParts.joined(separator: ", ")

        return AddressComponents(
            streetNumber: placemark.subThoroughfare,
            streetName: placemark.thoroughfare,
            city: placemark.locality,
            stateProvince: placemark.administrativeArea,
            postalCode: placemark.postalCode,
            countryCode: placemark.isoCountryCode,
            formattedAddress: formatted.isEmpty ? nil : formatted,
            latitude: placemark.location?.coordinate.latitude,
            longitude: placemark.location?.coordinate.longitude
        )
    }
}

// MARK: - CLLocationManagerDelegate

extension LocationService: CLLocationManagerDelegate {
    nonisolated func locationManagerDidChangeAuthorization(_ manager: CLLocationManager) {
        Task { @MainActor in
            authorizationStatus = manager.authorizationStatus
        }
    }

    nonisolated func locationManager(_ manager: CLLocationManager, didUpdateLocations locations: [CLLocation]) {
        Task { @MainActor in
            if let location = locations.last {
                currentLocation = location
            }
        }
    }

    nonisolated func locationManager(_ manager: CLLocationManager, didFailWithError error: Error) {
        Task { @MainActor in
            lastError = error
            isLoading = false
        }
    }
}

// MARK: - Errors

enum LocationError: LocalizedError {
    case notAuthorized
    case timeout
    case noPlacemark
    case geocodingFailed

    var errorDescription: String? {
        switch self {
        case .notAuthorized:
            return "Location access not authorized. Please enable location services in Settings."
        case .timeout:
            return "Location request timed out. Please try again."
        case .noPlacemark:
            return "Could not determine address from location."
        case .geocodingFailed:
            return "Failed to geocode address."
        }
    }
}
