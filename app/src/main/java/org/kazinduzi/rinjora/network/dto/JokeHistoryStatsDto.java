package org.kazinduzi.rinjora.network.dto;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * Aggregate attempt statistics from {@code GET /jokes/history/stats} (parity plan
 * §4.1): {@code { total_attempts, jokes_solved, unique_jokes, accuracy, by_category[] }}.
 * Unknown/optional fields degrade gracefully to defaults.
 */
public class JokeHistoryStatsDto {

    @SerializedName("total_attempts")
    private int totalAttempts;

    @SerializedName("jokes_solved")
    private int solved;

    @SerializedName("unique_jokes")
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
