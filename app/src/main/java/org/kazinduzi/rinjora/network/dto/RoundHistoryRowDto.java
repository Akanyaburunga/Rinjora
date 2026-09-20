package org.kazinduzi.rinjora.network.dto;

import com.google.gson.annotations.SerializedName;

/** One per-mode row of {@link RoundHistoryDto}: {@code { mode, games, points }}. */
public class RoundHistoryRowDto {

    @SerializedName("mode")
    private String mode;

    @SerializedName("games")
    private int games;

    @SerializedName("points")
    private int points;

    public String getMode() { return mode; }
    public int getGames() { return games; }
    public int getPoints() { return points; }
}