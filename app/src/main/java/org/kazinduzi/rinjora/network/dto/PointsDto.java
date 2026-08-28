package org.kazinduzi.rinjora.network.dto;

import com.google.gson.annotations.SerializedName;

/**
 * Points / level inside {@code GET /me/summary} (plan §4.1).
 */
public class PointsDto {

    @SerializedName("reputation")
    private int reputation;

    @SerializedName("level")
    private int level;

    public int getReputation() {
        return reputation;
    }

    public int getLevel() {
        return level;
    }
}
