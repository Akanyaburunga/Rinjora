package rw.martinhardware.mymartin.network.dto;

import com.google.gson.annotations.SerializedName;

/**
 * Body of {@code POST /riddles/streak/freeze} (plan §2.12):
 * {@code { freezes_remaining, freeze_active, streak }}. The backend returns 422
 * when no freeze remains or one is already active today.
 */
public class FreezeResponseDto {

    @SerializedName("freezes_remaining")
    private int freezesRemaining;

    @SerializedName("freeze_active")
    private boolean freezeActive;

    @SerializedName("streak")
    private StreakDto streak;

    public int getFreezesRemaining() { return freezesRemaining; }
    public boolean isFreezeActive() { return freezeActive; }
    public StreakDto getStreak() { return streak; }
}
