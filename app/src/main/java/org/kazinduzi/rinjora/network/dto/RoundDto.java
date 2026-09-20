package org.kazinduzi.rinjora.network.dto;

import com.google.gson.annotations.SerializedName;

/**
 * A game round on the server (parity plan §5): {@code { id, mode, level, item_count,
 * index, score, best_streak, current_streak, completed, has_more_levels, next_level,
 * level_available }}. The round owns the live score/streak state; the client is a
 * stateless viewer that never caches solved state.
 */
public class RoundDto {

    @SerializedName("id")
    private long id;

    @SerializedName("mode")
    private String mode;

    @SerializedName("level")
    private int level;

    @SerializedName("item_count")
    private int itemCount;

    @SerializedName("index")
    private int index;

    @SerializedName("score")
    private int score;

    @SerializedName("best_streak")
    private int bestStreak;

    @SerializedName("current_streak")
    private int currentStreak;

    @SerializedName("completed")
    private boolean completed;

    @SerializedName("has_more_levels")
    private boolean hasMoreLevels;

    @SerializedName("next_level")
    private int nextLevel;

    @SerializedName("level_available")
    private boolean levelAvailable;

    public long getId() { return id; }
    public String getMode() { return mode; }
    public int getLevel() { return level; }
    public int getItemCount() { return itemCount; }
    public int getIndex() { return index; }
    public int getScore() { return score; }
    public int getBestStreak() { return bestStreak; }
    public int getCurrentStreak() { return currentStreak; }
    public boolean isCompleted() { return completed; }
    public boolean isHasMoreLevels() { return hasMoreLevels; }
    public int getNextLevel() { return nextLevel; }
    public boolean isLevelAvailable() { return levelAvailable; }
}