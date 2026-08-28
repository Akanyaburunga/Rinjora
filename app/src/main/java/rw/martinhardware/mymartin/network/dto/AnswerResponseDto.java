package rw.martinhardware.mymartin.network.dto;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * Response body of {@code POST /riddles/{id}/answer} (plan §2.9).
 * {@code correct=false} → no points; {@code new_achievements} may be non-empty
 * on the first correct solve that unlocks badges.
 */
public class AnswerResponseDto {

    @SerializedName("correct")
    private boolean correct;

    @SerializedName("rewarded")
    private boolean rewarded;

    @SerializedName("points")
    private int points;

    @SerializedName("capped")
    private boolean capped;

    @SerializedName("message")
    private String message;

    @SerializedName("new_achievements")
    private List<AchievementDto> newAchievements;

    public boolean isCorrect() { return correct; }
    public boolean isRewarded() { return rewarded; }
    public int getPoints() { return points; }
    public boolean isCapped() { return capped; }
    public String getMessage() { return message; }

    public List<AchievementDto> getNewAchievements() {
        return newAchievements != null ? newAchievements : java.util.Collections.<AchievementDto>emptyList();
    }
}
