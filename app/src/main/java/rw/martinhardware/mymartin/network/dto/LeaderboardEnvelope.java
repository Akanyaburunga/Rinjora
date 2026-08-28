package rw.martinhardware.mymartin.network.dto;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * Full top-level body of {@code GET /leaderboard} (plan §5.1). Unlike the normal
 * {@code ApiEnvelope}, the leaderboard carries extra top-level fields
 * ({@code filter}, {@code me}, {@code meta}) alongside {@code success} and {@code data}.
 */
public class LeaderboardEnvelope {

    @SerializedName("success")
    private boolean success;

    @SerializedName("filter")
    private String filter;

    @SerializedName("data")
    private List<LeaderboardEntryDto> data;

    @SerializedName("me")
    private LeaderboardMeDto me;

    @SerializedName("meta")
    private LeaderboardMetaDto meta;

    public boolean isSuccess() { return success; }
    public String getFilter() { return filter; }

    public List<LeaderboardEntryDto> getData() {
        return data != null ? data : java.util.Collections.<LeaderboardEntryDto>emptyList();
    }

    public LeaderboardMeDto getMe() { return me; }
    public LeaderboardMetaDto getMeta() { return meta; }
}
