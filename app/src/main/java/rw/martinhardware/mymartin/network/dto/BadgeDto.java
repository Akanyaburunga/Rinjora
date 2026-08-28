package rw.martinhardware.mymartin.network.dto;

import com.google.gson.annotations.SerializedName;

/**
 * One entry of the achievements/badges library from {@code GET /me/achievements}
 * (plan §4.4): the full lifecycle shape with earned state and progress.
 */
public class BadgeDto {

    @SerializedName("id")
    private long id;

    @SerializedName("slug")
    private String slug;

    @SerializedName("name")
    private String name;

    @SerializedName("description")
    private String description;

    @SerializedName("category")
    private String category;

    @SerializedName("icon")
    private String icon;

    @SerializedName("threshold")
    private int threshold;

    @SerializedName("metric")
    private String metric;

    @SerializedName("earned")
    private boolean earned;

    @SerializedName("earned_at")
    private String earnedAt;

    @SerializedName("progress")
    private int progress;

    @SerializedName("goal")
    private int goal;

    public long getId() { return id; }
    public String getSlug() { return slug; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public String getCategory() { return category; }
    public String getIcon() { return icon; }
    public int getThreshold() { return threshold; }
    public String getMetric() { return metric; }
    public boolean isEarned() { return earned; }
    public String getEarnedAt() { return earnedAt; }
    public int getProgress() { return progress; }
    public int getGoal() { return goal; }
}
