package org.kazinduzi.rinjora.network.dto;

import com.google.gson.annotations.SerializedName;

/**
 * Payload of {@code GET /api/me} (parity plan §5): the currently logged-in user's
 * live round-backed stats {@code { name, points:{ reputation, level:{ level, title } },
 * streak:{ current, longest }, ... }}. This is the authoritative profile source for
 * the Jewe tab — the legacy {@code /me/summary} drives riddle-era counters only.
 */
public class MeDto {

    @SerializedName("name")
    private String name;

    @SerializedName("points")
    private MePointsDto points;

    @SerializedName("streak")
    private StreakDto streak;

    public String getName() { return name; }
    public MePointsDto getPoints() { return points; }
    public StreakDto getStreak() { return streak; }
}