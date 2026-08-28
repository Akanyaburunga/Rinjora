package org.kazinduzi.rinjora.models;

public class TicketMessage {
    private int id;
    private String message;
    private String createdAt;
    private AuthorSummary author;

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
    public AuthorSummary getAuthor() { return author; }
    public void setAuthor(AuthorSummary author) { this.author = author; }

    public static class AuthorSummary {
        private int id;
        private String name;
        private String email;
        private String type;
        public int getId() { return id; }
        public void setId(int id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
    }
}
