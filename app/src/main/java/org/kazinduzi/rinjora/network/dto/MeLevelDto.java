package org.kazinduzi.rinjora.network.dto;

import com.google.gson.annotations.SerializedName;

/**
 * Level inside {@code GET /api/me} (parity plan §5): {@code { level, title, ... }}.
 * The probe uses the numeric {@code level}; the title is carried for display parity.
 */
public class MeLevelDto {

    @SerializedName("level")
    private int level;

    @SerializedName("title")
    private String title;

    public int getLevel() { return level; }
    public String getTitle() { return title; }
}