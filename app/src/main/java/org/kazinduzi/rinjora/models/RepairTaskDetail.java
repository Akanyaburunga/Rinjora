package org.kazinduzi.rinjora.models;

public class RepairTaskDetail {
    private RepairTask assignment;
    private RepairRequest repairRequest;

    public RepairTask getAssignment() { return assignment; }
    public void setAssignment(RepairTask assignment) { this.assignment = assignment; }
    public RepairRequest getRepairRequest() { return repairRequest; }
    public void setRepairRequest(RepairRequest repairRequest) { this.repairRequest = repairRequest; }
}
