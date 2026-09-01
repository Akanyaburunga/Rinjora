package org.kazinduzi.rinjora.network.dto;

import com.google.gson.annotations.SerializedName;

/**
 * A riddle tag as returned by the Rinjora (Kazinduzi) API. The backend serialises
 * tags as objects ({@code { id, name, slug }}), not plain strings — see
 * {@code GameController::gamePayload} in the Laravel backend.
 */
public class TagDto {

    @SerializedName("id")
    private long id;

    @SerializedName("name")
    private String name;

    @SerializedName("slug")
    private String slug;

    public long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getSlug() {
        return slug;
    }
}
