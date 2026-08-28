package org.kazinduzi.rinjora.network.dto;

import com.google.gson.annotations.SerializedName;

/**
 * Body of {@code GET /riddles/daily} (plan §2.4/§2.5):
 * {@code { streak, solved_by_count, best_streak, daily }}.
 * The {@code daily} is a {@link RiddleDto} for today's (or a past date's) riddle.
 */
public class DailyRiddleDto {

    @SerializedName("streak")
    private StreakDto streak;

    @SerializedName("solved_by_count")
    private int solvedByCount;

    @SerializedName("best_streak")
    private int bestStreak;

    @SerializedName("daily")
    private RiddleDto daily;

    public StreakDto getStreak() { return streak; }
    public int getSolvedByCount() { return solvedByCount; }
    public int getBestStreak() { return bestStreak; }
    public RiddleDto getDaily() { return daily; }
}
