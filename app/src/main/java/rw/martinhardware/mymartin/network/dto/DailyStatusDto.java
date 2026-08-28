package rw.martinhardware.mymartin.network.dto;

import com.google.gson.annotations.SerializedName;

/**
 * Body of {@code GET /riddles/daily/status} (plan §2.6):
 * {@code { daily_available, streak_at_risk, pending_challenges, streak }}.
 * Drives the "Solve today" button, a streak-at-risk warning, and the duels inbox badge.
 */
public class DailyStatusDto {

    @SerializedName("daily_available")
    private boolean dailyAvailable;

    @SerializedName("streak_at_risk")
    private boolean streakAtRisk;

    @SerializedName("pending_challenges")
    private int pendingChallenges;

    @SerializedName("streak")
    private StreakDto streak;

    public boolean isDailyAvailable() { return dailyAvailable; }
    public boolean isStreakAtRisk() { return streakAtRisk; }
    public int getPendingChallenges() { return pendingChallenges; }
    public StreakDto getStreak() { return streak; }
}
