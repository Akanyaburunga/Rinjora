package org.kazinduzi.rinjora.models;

public class RepairTask {
    private int id;
    private int repairRequestId;
    private String reference;
    private String vehiclePlate;
    private String instructions;
    private String status;
    private String duration;
    private String assignedAt;
    private String startedAt;
    private String completedAt;
    private String completedNote;

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getRepairRequestId() { return repairRequestId; }
    public void setRepairRequestId(int repairRequestId) { this.repairRequestId = repairRequestId; }
    public String getReference() { return reference; }
    public void setReference(String reference) { this.reference = reference; }
    public String getVehiclePlate() { return vehiclePlate; }
    public void setVehiclePlate(String vehiclePlate) { this.vehiclePlate = vehiclePlate; }
    public String getInstructions() { return instructions; }
    public void setInstructions(String instructions) { this.instructions = instructions; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getDuration() { return duration; }
    public void setDuration(String duration) { this.duration = duration; }
    public String getAssignedAt() { return assignedAt; }
    public void setAssignedAt(String assignedAt) { this.assignedAt = assignedAt; }
    public String getStartedAt() { return startedAt; }
    public void setStartedAt(String startedAt) { this.startedAt = startedAt; }
    public String getCompletedAt() { return completedAt; }
    public void setCompletedAt(String completedAt) { this.completedAt = completedAt; }
    public String getCompletedNote() { return completedNote; }
    public void setCompletedNote(String completedNote) { this.completedNote = completedNote; }
}
