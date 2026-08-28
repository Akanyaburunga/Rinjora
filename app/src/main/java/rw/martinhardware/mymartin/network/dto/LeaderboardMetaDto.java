package rw.martinhardware.mymartin.network.dto;

import com.google.gson.annotations.SerializedName;

/**
 * The {@code meta} block of {@code GET /leaderboard} (plan §5.1) — pagination info:
 * {@code { current_page, per_page, total, last_page }}.
 */
public class LeaderboardMetaDto {

    @SerializedName("current_page")
    private int currentPage;

    @SerializedName("per_page")
    private int perPage;

    @SerializedName("total")
    private int total;

    @SerializedName("last_page")
    private int lastPage;

    public int getCurrentPage() { return currentPage; }
    public int getPerPage() { return perPage; }
    public int getTotal() { return total; }
    public int getLastPage() { return lastPage; }
}
