package rw.martinhardware.mymartin.network.dto;

import com.google.gson.annotations.SerializedName;

/**
 * Activity stats inside {@code GET /me/summary} (plan §4.1).
 */
public class ActivityDto {

    @SerializedName("total_attempts")
    private int totalAttempts;

    @SerializedName("riddles_solved")
    private int riddlesSolved;

    @SerializedName("accuracy")
    private double accuracy;

    @SerializedName("unique_riddles")
    private int uniqueRiddles;

    @SerializedName("submissions_count")
    private int submissionsCount;

    @SerializedName("shares_count")
    private int sharesCount;

    public int getTotalAttempts() {
        return totalAttempts;
    }

    public int getRiddlesSolved() {
        return riddlesSolved;
    }

    public double getAccuracy() {
        return accuracy;
    }

    public int getUniqueRiddles() {
        return uniqueRiddles;
    }

    public int getSubmissionsCount() {
        return submissionsCount;
    }

    public int getSharesCount() {
        return sharesCount;
    }
}
