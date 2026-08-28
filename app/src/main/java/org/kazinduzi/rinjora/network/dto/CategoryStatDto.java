package org.kazinduzi.rinjora.network.dto;

import com.google.gson.annotations.SerializedName;

/**
 * Per-category aggregate inside {@code GET /riddles/history/stats} (plan §2.11):
 * {@code { category_id, name, attempts, solved }}.
 */
public class CategoryStatDto {

    @SerializedName("category_id")
    private long categoryId;

    @SerializedName("name")
    private String name;

    @SerializedName("attempts")
    private int attempts;

    @SerializedName("solved")
    private int solved;

    public long getCategoryId() { return categoryId; }
    public String getName() { return name; }
    public int getAttempts() { return attempts; }
    public int getSolved() { return solved; }
}
