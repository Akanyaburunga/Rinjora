package org.kazinduzi.rinjora.network.dto;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * Aggregate attempt statistics from {@code GET /proverbs/history/stats} (parity plan
 * §4.1): {@code { total_attempts, proverbs_solved, unique_proverbs, accuracy,
 * by_category[] }}. Unknown/optional fields degrade gracefully to defaults.
 */
public class ProverbHistoryStatsDto {

    @SerializedName("total_attempts")
    private int totalAttempts;

    @SerializedName("proverbs_solved")
    private int solved;

    @SerializedName("unique_proverbs")
    private int unique;

    @SerializedName("accuracy")
    private double accuracy;

    @SerializedName("by_category")
    private List<CategoryStatDto> byCategory;

    public int getTotalAttempts() { return totalAttempts; }
    public int getSolved() { return solved; }
    public int getUnique() { return unique; }
    public double getAccuracy() { return accuracy; }

    public List<CategoryStatDto> getByCategory() {
        return byCategory != null ? byCategory : java.util.Collections.<CategoryStatDto>emptyList();
    }
}
