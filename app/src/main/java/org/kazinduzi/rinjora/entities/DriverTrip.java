package org.kazinduzi.rinjora.entities;

import io.objectbox.annotation.Entity;
import io.objectbox.annotation.Id;
import io.objectbox.annotation.Index;

/**
 * One row per recent trip shown on the profile screen (GET /api/mobile/profile latest_trips).
 * Replaced wholesale on every sync, keyed by driverId.
 */
@Entity
public class DriverTrip {

    @Id
    public long id;

    @Index
    public long driverId;

    public long tripId;
    public String reference;
    public String status;
    public String origin;
    public String destination;
    public String endedAt;

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public long getDriverId() { return driverId; }
    public void setDriverId(long driverId) { this.driverId = driverId; }
    public long getTripId() { return tripId; }
    public void setTripId(long tripId) { this.tripId = tripId; }
    public String getReference() { return reference; }
    public void setReference(String reference) { this.reference = reference; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getOrigin() { return origin; }
    public void setOrigin(String origin) { this.origin = origin; }
    public String getDestination() { return destination; }
    public void setDestination(String destination) { this.destination = destination; }
    public String getEndedAt() { return endedAt; }
    public void setEndedAt(String endedAt) { this.endedAt = endedAt; }
}
