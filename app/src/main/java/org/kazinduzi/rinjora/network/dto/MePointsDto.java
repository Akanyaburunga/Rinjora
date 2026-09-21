package org.kazinduzi.rinjora.network.dto;

import com.google.gson.annotations.SerializedName;

/**
 * Points block inside {@code GET /api/me} (parity plan §5): unlike the legacy
 * {@code /me/summary}, the level here is a nested object {@code { level, title }}.
 */
public class MePointsDto {

    @SerializedName("reputation")
    private int reputation;

    @SerializedName("level")
    private MeLevelDto level;

    public int getReputation() { return reputation; }
    public MeLevelDto getLevel() { return level; }
}