package org.kazinduzi.rinjora.network.dto;

import com.google.gson.annotations.SerializedName;

/**
 * The {@code me} block of {@code GET /leaderboard} (plan §5.1):
 * {@code { id, name, rank, points, total_players, percentile }}.
 * Renders a highlight row even when the current user isn't on the visible page.
 */
public class LeaderboardMeDto {

    @SerializedName("id")
    private long id;

    @SerializedName("name")
    private String name;

    @SerializedName("rank")
    private int rank;

    @SerializedName("points")
    private int points;

    @SerializedName("total_players")
    private int totalPlayers;

    @SerializedName("percentile")
    private double percentile;

    public long getId() { return id; }
    public String getName() { return name; }
    public int getRank() { return rank; }
    public int getPoints() { return points; }
    public int getTotalPlayers() { return totalPlayers; }
    public double getPercentile() { return percentile; }
}
