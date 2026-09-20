package org.kazinduzi.rinjora.network.dto;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * Payload of {@code GET /api/games/history} (parity plan §4.6):
 * {@code { total, games, best, rows:[{mode,games,points}] }}.
 */
public class RoundHistoryDto {

    @SerializedName("total")
    private int total;

    @SerializedName("games")
    private int games;

    @SerializedName("best")
    private int best;

    @SerializedName("rows")
    private List<RoundHistoryRowDto> rows;

    public int getTotal() { return total; }
    public int getGames() { return games; }
    public int getBest() { return best; }

    public List<RoundHistoryRowDto> getRows() {
        return rows != null ? rows : java.util.Collections.<RoundHistoryRowDto>emptyList();
    }
}