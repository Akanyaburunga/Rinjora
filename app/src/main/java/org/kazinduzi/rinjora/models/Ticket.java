package org.kazinduzi.rinjora.models;

import java.util.List;

public class Ticket {
    private int id;
    private String reference;
    private int supportCategoryId;
    private Integer assignedTo;
    private String title;
    private String description;
    private String priority;
    private String status;
    private String createdAt;
    private String updatedAt;
    private String resolvedAt;
    private String closedAt;
    private UserSummary user;
    private Category category;
    private UserSummary assignee;
    private List<TicketMessage> messages;
    private List<Event> events;
    private Object subject;

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getReference() { return reference; }
    public void setReference(String reference) { this.reference = reference; }
    public int getSupportCategoryId() { return supportCategoryId; }
    public void setSupportCategoryId(int supportCategoryId) { this.supportCategoryId = supportCategoryId; }
    public Integer getAssignedTo() { return assignedTo; }
    public void setAssignedTo(Integer assignedTo) { this.assignedTo = assignedTo; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
    public String getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }
    public String getResolvedAt() { return resolvedAt; }
    public void setResolvedAt(String resolvedAt) { this.resolvedAt = resolvedAt; }
    public String getClosedAt() { return closedAt; }
    public void setClosedAt(String closedAt) { this.closedAt = closedAt; }
    public UserSummary getUser() { return user; }
    public void setUser(UserSummary user) { this.user = user; }
    public Category getCategory() { return category; }
    public void setCategory(Category category) { this.category = category; }
    public UserSummary getAssignee() { return assignee; }
    public void setAssignee(UserSummary assignee) { this.assignee = assignee; }
    public List<TicketMessage> getMessages() { return messages; }
    public void setMessages(List<TicketMessage> messages) { this.messages = messages; }
    public List<Event> getEvents() { return events; }
    public void setEvents(List<Event> events) { this.events = events; }
    public Object getSubject() { return subject; }
    public void setSubject(Object subject) { this.subject = subject; }

    public static class UserSummary {
        private int id;
        private String name;
        private String email;
        public int getId() { return id; }
        public void setId(int id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
    }

    public static class Category {
        private int id;
        private String name;
        public int getId() { return id; }
        public void setId(int id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
    }

    public static class Event {
        private int id;
        private String type;
        private Object payload;
        private String createdAt;
        private UserSummary actor;
        public int getId() { return id; }
        public void setId(int id) { this.id = id; }
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        public Object getPayload() { return payload; }
        public void setPayload(Object payload) { this.payload = payload; }
        public String getCreatedAt() { return createdAt; }
        public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
        public UserSummary getActor() { return actor; }
        public void setActor(UserSummary actor) { this.actor = actor; }
    }
}
