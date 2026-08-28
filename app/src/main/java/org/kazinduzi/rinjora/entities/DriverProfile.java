package org.kazinduzi.rinjora.entities;

import java.util.List;

import io.objectbox.annotation.Entity;
import io.objectbox.annotation.Id;
import io.objectbox.annotation.Index;
import io.objectbox.annotation.Transient;

/**
 * Offline-first cache for the driver profile screen (GET /api/mobile/profile).
 * One row per driver, overwritten on every sync. UI reads this first so the profile
 * renders instantly offline, then refreshes in the background.
 */
@Entity
public class DriverProfile {

    @Id
    public long id;

    @Index
    public long driverId;

    /** ms epoch of the last successful sync. */
    public long fetchedAt;

    /** Parsed recent trips, persisted to their own entity by the repository. */
    @Transient
    public List<DriverTrip> trips;

    // --- driver ---
    public String driverName;
    public String driverEmail;
    public String driverPhone;
    public String driverWhatsapp;
    public String driverNationality;
    public String driverBranch;
    public double rating;
    public int ratingCount;
    public String memberSince;

    // --- vehicle (truck + trailer) ---
    public long vehicleId;
    public String plateNumber;
    public String vehicleMake;
    public String vehicleModel;
    public String fuelType;
    public String vehicleStatus;
    public String trailerPlate;

    // --- stats ---
    public int totalTrips;
    public int completedTrips;
    public int pendingTrips;
    public double totalDistanceKm;
    public double hoursDriven;

    /** Raw JSON payload, kept for future screens / debugging. */
    public String rawJson;

    // --- Getters & setters ---
    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public long getDriverId() { return driverId; }
    public void setDriverId(long driverId) { this.driverId = driverId; }
    public long getFetchedAt() { return fetchedAt; }
    public void setFetchedAt(long fetchedAt) { this.fetchedAt = fetchedAt; }

    public String getDriverName() { return driverName; }
    public void setDriverName(String driverName) { this.driverName = driverName; }
    public String getDriverEmail() { return driverEmail; }
    public void setDriverEmail(String driverEmail) { this.driverEmail = driverEmail; }
    public String getDriverPhone() { return driverPhone; }
    public void setDriverPhone(String driverPhone) { this.driverPhone = driverPhone; }
    public String getDriverWhatsapp() { return driverWhatsapp; }
    public void setDriverWhatsapp(String driverWhatsapp) { this.driverWhatsapp = driverWhatsapp; }
    public String getDriverNationality() { return driverNationality; }
    public void setDriverNationality(String driverNationality) { this.driverNationality = driverNationality; }
    public String getDriverBranch() { return driverBranch; }
    public void setDriverBranch(String driverBranch) { this.driverBranch = driverBranch; }
    public double getRating() { return rating; }
    public void setRating(double rating) { this.rating = rating; }
    public int getRatingCount() { return ratingCount; }
    public void setRatingCount(int ratingCount) { this.ratingCount = ratingCount; }
    public String getMemberSince() { return memberSince; }
    public void setMemberSince(String memberSince) { this.memberSince = memberSince; }

    public long getVehicleId() { return vehicleId; }
    public void setVehicleId(long vehicleId) { this.vehicleId = vehicleId; }
    public String getPlateNumber() { return plateNumber; }
    public void setPlateNumber(String plateNumber) { this.plateNumber = plateNumber; }
    public String getVehicleMake() { return vehicleMake; }
    public void setVehicleMake(String vehicleMake) { this.vehicleMake = vehicleMake; }
    public String getVehicleModel() { return vehicleModel; }
    public void setVehicleModel(String vehicleModel) { this.vehicleModel = vehicleModel; }
    public String getFuelType() { return fuelType; }
    public void setFuelType(String fuelType) { this.fuelType = fuelType; }
    public String getVehicleStatus() { return vehicleStatus; }
    public void setVehicleStatus(String vehicleStatus) { this.vehicleStatus = vehicleStatus; }
    public String getTrailerPlate() { return trailerPlate; }
    public void setTrailerPlate(String trailerPlate) { this.trailerPlate = trailerPlate; }

    public int getTotalTrips() { return totalTrips; }
    public void setTotalTrips(int totalTrips) { this.totalTrips = totalTrips; }
    public int getCompletedTrips() { return completedTrips; }
    public void setCompletedTrips(int completedTrips) { this.completedTrips = completedTrips; }
    public int getPendingTrips() { return pendingTrips; }
    public void setPendingTrips(int pendingTrips) { this.pendingTrips = pendingTrips; }
    public double getTotalDistanceKm() { return totalDistanceKm; }
    public void setTotalDistanceKm(double totalDistanceKm) { this.totalDistanceKm = totalDistanceKm; }
    public double getHoursDriven() { return hoursDriven; }
    public void setHoursDriven(double hoursDriven) { this.hoursDriven = hoursDriven; }

    public String getRawJson() { return rawJson; }
    public void setRawJson(String rawJson) { this.rawJson = rawJson; }
}
