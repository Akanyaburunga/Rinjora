package org.kazinduzi.rinjora.network.dto;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * A riddle as returned by the Rinjora (Kazinduzi) API (plan §2.1).
 * <p>
 * Note: the backend intentionally omits {@code answer} until the player solves
 * that riddle, so this DTO never binds it from list/detail payloads.
 */
public class RiddleDto {

    @SerializedName("id")
    private long id;

    @SerializedName("solved")
    private boolean solved;

    @SerializedName("hints_revealed")
    private int hintsRevealed;

    @SerializedName("category")
    private CategoryDto category;

    @SerializedName("question")
    private String question;

    @SerializedName("difficulty")
    private String difficulty;

    @SerializedName("riddle_type")
    private String riddleType;

    @SerializedName("tags")
    private List<String> tags;

    @SerializedName("hint")
    private String hint;

    @SerializedName("hint2")
    private String hint2;

    @SerializedName("created_at")
    private String createdAt;

    public long getId() {
        return id;
    }

    public boolean isSolved() {
        return solved;
    }

    public int getHintsRevealed() {
        return hintsRevealed;
    }

    public CategoryDto getCategory() {
        return category;
    }

    public String getQuestion() {
        return question;
    }

    public String getDifficulty() {
        return difficulty;
    }

    public String getRiddleType() {
        return riddleType;
    }

    public List<String> getTags() {
        return tags;
    }

    public String getHint() {
        return hint;
    }

    public String getHint2() {
        return hint2;
    }

    public String getCreatedAt() {
        return createdAt;
    }
}
