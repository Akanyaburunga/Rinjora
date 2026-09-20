package org.kazinduzi.rinjora.network.dto;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * Flat grade returned by the per-item answer/skip endpoints
 * (parity plan §5): {@code { correct, conceded, answer?, message, rewarded, points,
 * capped, round{...}, new_achievements:[] }}. Not wrapped in the {@code ApiEnvelope}.
 *
 * <p>{@code answer} is the canonical answer/punchline and is present once the item is
 * solved or conceded (used to reveal the answer); stayed null otherwise.
 */
public class RoundAnswerDto {

    @SerializedName("correct")
    private boolean correct;

    @SerializedName("conceded")
    private boolean conceded;

    @SerializedName("answer")
    private String answer;

    @SerializedName("message")
    private String message;

    @SerializedName("rewarded")
    private boolean rewarded;

    @SerializedName("points")
    private int points;

    @SerializedName("capped")
    private boolean capped;

    @SerializedName("round")
    private RoundDto round;

    @SerializedName("new_achievements")
    private List<AchievementDto> newAchievements;

    public boolean isCorrect() { return correct; }
    public boolean isConceded() { return conceded; }
    public String getAnswer() { return answer; }
    public String getMessage() { return message; }
    public boolean isRewarded() { return rewarded; }
    public int getPoints() { return points; }
    public boolean isCapped() { return capped; }
    public RoundDto getRound() { return round; }

    public List<AchievementDto> getNewAchievements() {
        return newAchievements != null ? newAchievements : java.util.Collections.<AchievementDto>emptyList();
    }
}