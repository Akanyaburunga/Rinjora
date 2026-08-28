package org.kazinduzi.rinjora.network.dto;

import com.google.gson.annotations.SerializedName;

/**
 * Payload of {@code GET /me/summary} — the single call that backs the Home
 * screen (plan §4.1). The app auto-refreshes it on resume.
 */
public class SummaryDto {

    @SerializedName("user")
    private SummaryUserDto user;

    @SerializedName("points")
    private PointsDto points;

    @SerializedName("streak")
    private StreakDto streak;

    @SerializedName("badges")
    private BadgesDto badges;

    @SerializedName("favorites_count")
    private int favoritesCount;

    @SerializedName("activity")
    private ActivityDto activity;

    public SummaryUserDto getUser() {
        return user;
    }

    public PointsDto getPoints() {
        return points;
    }

    public StreakDto getStreak() {
        return streak;
    }

    public BadgesDto getBadges() {
        return badges;
    }

    public int getFavoritesCount() {
        return favoritesCount;
    }

    public ActivityDto getActivity() {
        return activity;
    }
}
