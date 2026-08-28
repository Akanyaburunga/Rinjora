package org.kazinduzi.rinjora.models;

public class RepairVehicle {
    private int id;
    private String plateNumber;
    private String status;
    private int currentDriverId;

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getPlateNumber() { return plateNumber; }
    public void setPlateNumber(String plateNumber) { this.plateNumber = plateNumber; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public int getCurrentDriverId() { return currentDriverId; }
    public void setCurrentDriverId(int currentDriverId) { this.currentDriverId = currentDriverId; }
}
