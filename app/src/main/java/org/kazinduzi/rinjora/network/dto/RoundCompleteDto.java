package org.kazinduzi.rinjora.network.dto;

import com.google.gson.annotations.SerializedName;

/**
 * Payload of {@code POST /api/games/{mode}/rounds/{round}/complete}:
 * {@code { round:{...}, performance }} where {@code performance} is
 * {@code "top" | "mid" | "low"}.
 */
public class RoundCompleteDto {

    @SerializedName("round")
    private RoundDto round;

    @SerializedName("performance")
    private String performance;

    public RoundDto getRound() { return round; }
    public String getPerformance() { return performance; }
}