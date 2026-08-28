package rw.martinhardware.mymartin.network.dto;

import com.google.gson.annotations.SerializedName;

/**
 * A single ranked row on {@code GET /leaderboard} (plan §5.1):
 * {@code { rank, id, name, points, words_contributed, meanings_contributed, profile_picture_url }}.
 */
public class LeaderboardEntryDto {

    @SerializedName("rank")
    private int rank;

    @SerializedName("id")
    private long id;

    @SerializedName("name")
    private String name;

    @SerializedName("points")
    private int points;

    @SerializedName("words_contributed")
    private int wordsContributed;

    @SerializedName("meanings_contributed")
    private int meaningsContributed;

    @SerializedName("profile_picture_url")
    private String profilePictureUrl;

    public int getRank() { return rank; }
    public long getId() { return id; }
    public String getName() { return name; }
    public int getPoints() { return points; }
    public int getWordsContributed() { return wordsContributed; }
    public int getMeaningsContributed() { return meaningsContributed; }
    public String getProfilePictureUrl() { return profilePictureUrl; }
}
