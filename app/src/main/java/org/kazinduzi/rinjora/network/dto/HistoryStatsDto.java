package org.kazinduzi.rinjora.network.dto;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * Aggregate attempt statistics from {@code GET /riddles/history/stats} (plan §2.11):
 * {@code { total_attempts, riddles_solved, unique_riddles, accuracy, by_category[] }}.
 */
public class HistoryStatsDto {

    @SerializedName("total_attempts")
    private int totalAttempts;

    @SerializedName("riddles_solved")
    private int riddlesSolved;

    @SerializedName("unique_riddles")
    private int uniqueRiddles;

    @SerializedName("accuracy")
    private double accuracy;

    @SerializedName("by_category")
    private List<CategoryStatDto> byCategory;

    public int getTotalAttempts() { return totalAttempts; }
    public int getRiddlesSolved() { return riddlesSolved; }
    public int getUniqueRiddles() { return uniqueRiddles; }
    public double getAccuracy() { return accuracy; }

    public List<CategoryStatDto> getByCategory() {
        return byCategory != null ? byCategory : java.util.Collections.<CategoryStatDto>emptyList();
    }
}
