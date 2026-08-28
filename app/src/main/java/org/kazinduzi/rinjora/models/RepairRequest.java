package org.kazinduzi.rinjora.models;

import java.util.List;

public class RepairRequest {
    private int id;
    private String reference;
    private String type;
    private String priority;
    private String status;
    private String description;
    private double latitude;
    private double longitude;
    private String createdAt;
    private String updatedAt;
    private String submittedAt;
    private Vehicle vehicle;
    private Driver driver;
    private List<RepairItem> items;
    private List<Approval> approvals;
    private List<Assignment> assignments;
    private Release release;

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getReference() { return reference; }
    public void setReference(String reference) { this.reference = reference; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public double getLatitude() { return latitude; }
    public void setLatitude(double latitude) { this.latitude = latitude; }
    public double getLongitude() { return longitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }
    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
    public String getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }
    public String getSubmittedAt() { return submittedAt; }
    public void setSubmittedAt(String submittedAt) { this.submittedAt = submittedAt; }
    public Vehicle getVehicle() { return vehicle; }
    public void setVehicle(Vehicle vehicle) { this.vehicle = vehicle; }
    public Driver getDriver() { return driver; }
    public void setDriver(Driver driver) { this.driver = driver; }
    public List<RepairItem> getItems() { return items; }
    public void setItems(List<RepairItem> items) { this.items = items; }
    public List<Approval> getApprovals() { return approvals; }
    public void setApprovals(List<Approval> approvals) { this.approvals = approvals; }
    public List<Assignment> getAssignments() { return assignments; }
    public void setAssignments(List<Assignment> assignments) { this.assignments = assignments; }
    public Release getRelease() { return release; }
    public void setRelease(Release release) { this.release = release; }

    public static class Vehicle {
        private int id;
        private String plateNumber;
        private String status;
        public int getId() { return id; }
        public void setId(int id) { this.id = id; }
        public String getPlateNumber() { return plateNumber; }
        public void setPlateNumber(String plateNumber) { this.plateNumber = plateNumber; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
    }

    public static class Driver {
        private int id;
        private String name;
        private String phone;
        public int getId() { return id; }
        public void setId(int id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getPhone() { return phone; }
        public void setPhone(String phone) { this.phone = phone; }
    }

    public static class Approval {
        private int id;
        private String stage;
        private String status;
        private String comment;
        private String createdAt;
        private String actorName;
        public int getId() { return id; }
        public void setId(int id) { this.id = id; }
        public String getStage() { return stage; }
        public void setStage(String stage) { this.stage = stage; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public String getComment() { return comment; }
        public void setComment(String comment) { this.comment = comment; }
        public String getCreatedAt() { return createdAt; }
        public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
        public String getActorName() { return actorName; }
        public void setActorName(String actorName) { this.actorName = actorName; }
    }

    public static class Assignment {
        private int id;
        private String status;
        private String instructions;
        private String assignedAt;
        private String startedAt;
        private String completedAt;
        private String completedNote;
        private String mechanicName;
        public int getId() { return id; }
        public void setId(int id) { this.id = id; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public String getInstructions() { return instructions; }
        public void setInstructions(String instructions) { this.instructions = instructions; }
        public String getAssignedAt() { return assignedAt; }
        public void setAssignedAt(String assignedAt) { this.assignedAt = assignedAt; }
        public String getStartedAt() { return startedAt; }
        public void setStartedAt(String startedAt) { this.startedAt = startedAt; }
        public String getCompletedAt() { return completedAt; }
        public void setCompletedAt(String completedAt) { this.completedAt = completedAt; }
        public String getCompletedNote() { return completedNote; }
        public void setCompletedNote(String completedNote) { this.completedNote = completedNote; }
        public String getMechanicName() { return mechanicName; }
        public void setMechanicName(String mechanicName) { this.mechanicName = mechanicName; }
    }

    public static class Release {
        private int id;
        private boolean checklistCompleted;
        private String unresolvedIssues;
        private String odometerAtRelease;
        private String releasedAt;
        public int getId() { return id; }
        public void setId(int id) { this.id = id; }
        public boolean isChecklistCompleted() { return checklistCompleted; }
        public void setChecklistCompleted(boolean checklistCompleted) { this.checklistCompleted = checklistCompleted; }
        public String getUnresolvedIssues() { return unresolvedIssues; }
        public void setUnresolvedIssues(String unresolvedIssues) { this.unresolvedIssues = unresolvedIssues; }
        public String getOdometerAtRelease() { return odometerAtRelease; }
        public void setOdometerAtRelease(String odometerAtRelease) { this.odometerAtRelease = odometerAtRelease; }
        public String getReleasedAt() { return releasedAt; }
        public void setReleasedAt(String releasedAt) { this.releasedAt = releasedAt; }
    }
}
