package org.kazinduzi.rinjora.network.dto;

import com.google.gson.annotations.SerializedName;

/**
 * A user riddle submission from {@code GET /submissions/riddles} (plan §8.2).
 * {@code answer} is intentionally not bound for the list; status drives the UI and
 * {@code rejection_reason} explains why an admin rejected it.
 */
public class SubmissionDto {

    @SerializedName("id")
    private long id;

    @SerializedName("question")
    private String question;

    @SerializedName("status")
    private String status;

    @SerializedName("rejection_reason")
    private String rejectionReason;

    @SerializedName("difficulty")
    private String difficulty;

    @SerializedName("riddle_type")
    private String riddleType;

    @SerializedName("hint")
    private String hint;

    @SerializedName("hint2")
    private String hint2;

    @SerializedName("source")
    private String source;

    @SerializedName("created_at")
    private String createdAt;

    public long getId() { return id; }
    public String getQuestion() { return question; }
    public String getStatus() { return status; }
    public String getRejectionReason() { return rejectionReason; }
    public String getDifficulty() { return difficulty; }
    public String getRiddleType() { return riddleType; }
    public String getHint() { return hint; }
    public String getHint2() { return hint2; }
    public String getSource() { return source; }
    public String getCreatedAt() { return createdAt; }
}
