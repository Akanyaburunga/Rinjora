package rw.martinhardware.mymartin.network.dto;

import com.google.gson.annotations.SerializedName;

/**
 * Player profile as returned inside the login payload (plan §1.4) and by
 * {@code GET /auth/user} (plan §1.6). Only the subset needed is modelled.
 */
public class UserDto {

    @SerializedName("id")
    private long id;

    @SerializedName("name")
    private String name;

    @SerializedName("email")
    private String email;

    @SerializedName("profile_picture_url")
    private String profilePictureUrl;

    @SerializedName("reputation")
    private int reputation;

    @SerializedName("current_streak")
    private int currentStreak;

    @SerializedName("longest_streak")
    private int longestStreak;

    @SerializedName("streak_freezes")
    private int streakFreezes;

    public long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public String getProfilePictureUrl() {
        return profilePictureUrl;
    }

    public int getReputation() {
        return reputation;
    }

    public int getCurrentStreak() {
        return currentStreak;
    }

    public int getLongestStreak() {
        return longestStreak;
    }

    public int getStreakFreezes() {
        return streakFreezes;
    }
}
