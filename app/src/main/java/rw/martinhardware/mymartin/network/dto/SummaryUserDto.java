package rw.martinhardware.mymartin.network.dto;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * Player identity inside {@code GET /me/summary} (plan §4.1).
 */
public class SummaryUserDto {

    @SerializedName("id")
    private long id;

    @SerializedName("name")
    private String name;

    @SerializedName("profile_picture_url")
    private String profilePictureUrl;

    public long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getProfilePictureUrl() {
        return profilePictureUrl;
    }
}
