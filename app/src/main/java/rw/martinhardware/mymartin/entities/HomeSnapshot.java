package rw.martinhardware.mymartin.entities;

import io.objectbox.annotation.Entity;
import io.objectbox.annotation.Id;
import io.objectbox.annotation.Index;

/**
 * Offline-first cache for the driver home dashboard (GET /api/mobile/trips/current).
 * One row per driver, overwritten on every sync. UI reads this first so the home
 * screen renders instantly (even fully offline), then refreshes in the background.
 */
@Entity
public class HomeSnapshot {

    @Id
    public long id;

    @Index
    public long driverId;

    /** ms epoch of the last successful sync. */
    public long fetchedAt;

    /** False when the endpoint returned 404 (driver idle / no active trip). */
    public boolean hasActiveTrip;

    /** True when the response contained a `position` object (telemetry exists). */
    public boolean hasPosition;

    // --- driver ---
    public String driverName;
    public String driverEmail;
    public String driverPhone;
    public String driverWhatsapp;
    public String driverNationality;
    public String driverBranch;

    // --- vehicle (truck + trailer) ---
    public long vehicleId;
    public String plateNumber;
    public String vehicleMake;
    public String vehicleModel;
    public String fuelType;
    public double tankCapacity;
    public String vehicleStatus;
    public String trailerPlate;

    // --- position / fuel ---
    public double latitude;
    public double longitude;
    public double speed;
    public double fuelLevel;
    public String lastSeenAt;
    public boolean isMoving;
    public boolean ignition;
    public boolean isStale;

    // --- nearest place ---
    public long placeId;
    public String placeName;
    public String placeType;
    public String placeCity;
    public int placeDistanceMeters;
    public double placeLat;
    public double placeLng;

    // --- assigned staff ---
    public String staffName;
    public String staffRoles;
    public String staffPhone;
    public String staffWhatsapp;

    // --- trip ---
    public long tripId;
    public String tripReference;
    public String tripStatus;
    public String orderReference;
    public String orderOrigin;
    public String orderDestination;
    public String routeName;
    public double routeDistanceKm;

    /** Raw JSON payload, kept for future screens / debugging. */
    public String rawJson;

    // --- Getters & setters ---
    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public long getDriverId() { return driverId; }
    public void setDriverId(long driverId) { this.driverId = driverId; }
    public long getFetchedAt() { return fetchedAt; }
    public void setFetchedAt(long fetchedAt) { this.fetchedAt = fetchedAt; }
    public boolean isHasActiveTrip() { return hasActiveTrip; }
    public void setHasActiveTrip(boolean hasActiveTrip) { this.hasActiveTrip = hasActiveTrip; }
    public boolean isHasPosition() { return hasPosition; }
    public void setHasPosition(boolean hasPosition) { this.hasPosition = hasPosition; }

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
    public double getTankCapacity() { return tankCapacity; }
    public void setTankCapacity(double tankCapacity) { this.tankCapacity = tankCapacity; }
    public String getVehicleStatus() { return vehicleStatus; }
    public void setVehicleStatus(String vehicleStatus) { this.vehicleStatus = vehicleStatus; }
    public String getTrailerPlate() { return trailerPlate; }
    public void setTrailerPlate(String trailerPlate) { this.trailerPlate = trailerPlate; }

    public double getLatitude() { return latitude; }
    public void setLatitude(double latitude) { this.latitude = latitude; }
    public double getLongitude() { return longitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }
    public double getSpeed() { return speed; }
    public void setSpeed(double speed) { this.speed = speed; }
    public double getFuelLevel() { return fuelLevel; }
    public void setFuelLevel(double fuelLevel) { this.fuelLevel = fuelLevel; }
    public String getLastSeenAt() { return lastSeenAt; }
    public void setLastSeenAt(String lastSeenAt) { this.lastSeenAt = lastSeenAt; }
    public boolean isMoving() { return isMoving; }
    public void setMoving(boolean moving) { isMoving = moving; }
    public boolean isIgnition() { return ignition; }
    public void setIgnition(boolean ignition) { this.ignition = ignition; }
    public boolean isStale() { return isStale; }
    public void setStale(boolean stale) { isStale = stale; }

    public long getPlaceId() { return placeId; }
    public void setPlaceId(long placeId) { this.placeId = placeId; }
    public String getPlaceName() { return placeName; }
    public void setPlaceName(String placeName) { this.placeName = placeName; }
    public String getPlaceType() { return placeType; }
    public void setPlaceType(String placeType) { this.placeType = placeType; }
    public String getPlaceCity() { return placeCity; }
    public void setPlaceCity(String placeCity) { this.placeCity = placeCity; }
    public int getPlaceDistanceMeters() { return placeDistanceMeters; }
    public void setPlaceDistanceMeters(int placeDistanceMeters) { this.placeDistanceMeters = placeDistanceMeters; }
    public double getPlaceLat() { return placeLat; }
    public void setPlaceLat(double placeLat) { this.placeLat = placeLat; }
    public double getPlaceLng() { return placeLng; }
    public void setPlaceLng(double placeLng) { this.placeLng = placeLng; }

    public String getStaffName() { return staffName; }
    public void setStaffName(String staffName) { this.staffName = staffName; }
    public String getStaffRoles() { return staffRoles; }
    public void setStaffRoles(String staffRoles) { this.staffRoles = staffRoles; }
    public String getStaffPhone() { return staffPhone; }
    public void setStaffPhone(String staffPhone) { this.staffPhone = staffPhone; }
    public String getStaffWhatsapp() { return staffWhatsapp; }
    public void setStaffWhatsapp(String staffWhatsapp) { this.staffWhatsapp = staffWhatsapp; }

    public long getTripId() { return tripId; }
    public void setTripId(long tripId) { this.tripId = tripId; }
    public String getTripReference() { return tripReference; }
    public void setTripReference(String tripReference) { this.tripReference = tripReference; }
    public String getTripStatus() { return tripStatus; }
    public void setTripStatus(String tripStatus) { this.tripStatus = tripStatus; }
    public String getOrderReference() { return orderReference; }
    public void setOrderReference(String orderReference) { this.orderReference = orderReference; }
    public String getOrderOrigin() { return orderOrigin; }
    public void setOrderOrigin(String orderOrigin) { this.orderOrigin = orderOrigin; }
    public String getOrderDestination() { return orderDestination; }
    public void setOrderDestination(String orderDestination) { this.orderDestination = orderDestination; }
    public String getRouteName() { return routeName; }
    public void setRouteName(String routeName) { this.routeName = routeName; }
    public double getRouteDistanceKm() { return routeDistanceKm; }
    public void setRouteDistanceKm(double routeDistanceKm) { this.routeDistanceKm = routeDistanceKm; }

    public String getRawJson() { return rawJson; }
    public void setRawJson(String rawJson) { this.rawJson = rawJson; }
}
