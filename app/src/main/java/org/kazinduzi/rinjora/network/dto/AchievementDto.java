package org.kazinduzi.rinjora.network.dto;

import com.google.gson.annotations.SerializedName;

/**
 * A badge/achievement, e.g. returned inside {@code POST /riddles/{id}/answer} as
 * {@code new_achievements[]} (plan §2.9): {@code { slug, name, description, icon }}.
 */
public class AchievementDto {

    @SerializedName("slug")
    private String slug;

    @SerializedName("name")
    private String name;

    @SerializedName("description")
    private String description;

    @SerializedName("icon")
    private String icon;

    public String getSlug() { return slug; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public String getIcon() { return icon; }
}
