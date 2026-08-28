package org.kazinduzi.rinjora.network.dto;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * Payload of {@code GET /me/achievements} (plan §4.4): a summary of earned
 * badges plus the full library (earned state + per-badge progress).
 */
public class AchievementLibraryDto {

    @SerializedName("earned_count")
    private int earnedCount;

    @SerializedName("total")
    private int total;

    @SerializedName("achievements")
    private List<BadgeDto> achievements;

    public int getEarnedCount() { return earnedCount; }
    public int getTotal() { return total; }
    public List<BadgeDto> getAchievements() { return achievements; }
}
