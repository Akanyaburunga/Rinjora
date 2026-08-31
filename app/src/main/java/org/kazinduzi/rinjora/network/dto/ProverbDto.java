package org.kazinduzi.rinjora.network.dto;

import com.google.gson.annotations.SerializedName;

/**
 * A proverb as returned by the Rinjora proverbs (Heraheza) API (parity plan §2.2):
 * {@code { id, solved, category, question, difficulty, source, created_at }}.
 * <p>
 * The backend intentionally omits {@code answer} until the player solves or reveals
 * the proverb, so this DTO never binds it from list/detail payloads.
 */
public class ProverbDto {

    @SerializedName("id")
    private long id;

    @SerializedName("solved")
    private boolean solved;

    @SerializedName("category")
    private CategoryDto category;

    @SerializedName("question")
    private String question;

    @SerializedName("difficulty")
    private String difficulty;

    @SerializedName("source")
    private String source;

    @SerializedName("created_at")
    private String createdAt;

    public long getId() { return id; }
    public boolean isSolved() { return solved; }
    public CategoryDto getCategory() { return category; }
    public String getQuestion() { return question; }
    public String getDifficulty() { return difficulty; }
    public String getSource() { return source; }
    public String getCreatedAt() { return createdAt; }
}
