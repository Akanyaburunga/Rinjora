package rw.martinhardware.mymartin.network.dto;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * Badges inside {@code GET /me/summary} (plan §4.1).
 */
public class BadgesDto {

    @SerializedName("earned_count")
    private int earnedCount;

    @SerializedName("total")
    private int total;

    @SerializedName("earned_slugs")
    private List<String> earnedSlugs;

    public int getEarnedCount() {
        return earnedCount;
    }

    public int getTotal() {
        return total;
    }

    public List<String> getEarnedSlugs() {
        return earnedSlugs;
    }
}
