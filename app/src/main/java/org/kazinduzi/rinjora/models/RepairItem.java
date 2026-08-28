package org.kazinduzi.rinjora.models;

public class RepairItem {
    private int id;
    private String description;
    private double estimatedQuantity;
    private double estimatedUnitPrice;
    private double estimatedTotal;
    private double actualQuantity;
    private double actualUnitPrice;
    private double actualTotal;
    private String status;
    private Part part;

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public double getEstimatedQuantity() { return estimatedQuantity; }
    public void setEstimatedQuantity(double estimatedQuantity) { this.estimatedQuantity = estimatedQuantity; }
    public double getEstimatedUnitPrice() { return estimatedUnitPrice; }
    public void setEstimatedUnitPrice(double estimatedUnitPrice) { this.estimatedUnitPrice = estimatedUnitPrice; }
    public double getEstimatedTotal() { return estimatedTotal; }
    public void setEstimatedTotal(double estimatedTotal) { this.estimatedTotal = estimatedTotal; }
    public double getActualQuantity() { return actualQuantity; }
    public void setActualQuantity(double actualQuantity) { this.actualQuantity = actualQuantity; }
    public double getActualUnitPrice() { return actualUnitPrice; }
    public void setActualUnitPrice(double actualUnitPrice) { this.actualUnitPrice = actualUnitPrice; }
    public double getActualTotal() { return actualTotal; }
    public void setActualTotal(double actualTotal) { this.actualTotal = actualTotal; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Part getPart() { return part; }
    public void setPart(Part part) { this.part = part; }

    public static class Part {
        private int id;
        private String name;
        private String sku;
        private double unitPrice;
        private String unitOfMeasure;
        public int getId() { return id; }
        public void setId(int id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getSku() { return sku; }
        public void setSku(String sku) { this.sku = sku; }
        public double getUnitPrice() { return unitPrice; }
        public void setUnitPrice(double unitPrice) { this.unitPrice = unitPrice; }
        public String getUnitOfMeasure() { return unitOfMeasure; }
        public void setUnitOfMeasure(String unitOfMeasure) { this.unitOfMeasure = unitOfMeasure; }
    }
}
