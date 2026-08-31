package org.kazinduzi.rinjora.network.dto;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * Response body of {@code POST /jokes/{id}/answer} (parity plan §3.2).
 * <p>
 * Correct → {@code { success:true, correct:true, rewarded:true, points, message,
 * new_achievements }}; the {@code answer} field is absent so {@link #getAnswer()}
 * stays null (no confidentiality leak).
 * <p>
 * Wrong → {@code { success:false, correct:false, message, answer:"<correct punchline>" }}
 * — this DTO binds {@code answer} so the UI can reveal which option was right, but it is
 * only ever populated after a submitted attempt (never pre-fetched for an unsolved round).
 */
public class JokeAnswerResponseDto {

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

    @SerializedName("answer")
    private String answer;

    @SerializedName("new_achievements")
    private List<AchievementDto> newAchievements;

    public boolean isCorrect() { return correct; }
    public boolean isRewarded() { return rewarded; }
    public int getPoints() { return points; }
    public boolean isCapped() { return capped; }
    public String getMessage() { return message; }
    public String getAnswer() { return answer; }

    public List<AchievementDto> getNewAchievements() {
        return newAchievements != null ? newAchievements : java.util.Collections.<AchievementDto>emptyList();
    }
}
