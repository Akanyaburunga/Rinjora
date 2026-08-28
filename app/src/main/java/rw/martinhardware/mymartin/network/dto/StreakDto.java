package rw.martinhardware.mymartin.network.dto;

import com.google.gson.annotations.SerializedName;

/**
 * Streak inside {@code GET /me/summary} (plan §4.1).
 */
public class StreakDto {

    @SerializedName("current")
    private int current;

    @SerializedName("longest")
    private int longest;

    public int getCurrent() {
        return current;
    }

    public int getLongest() {
        return longest;
    }
}
