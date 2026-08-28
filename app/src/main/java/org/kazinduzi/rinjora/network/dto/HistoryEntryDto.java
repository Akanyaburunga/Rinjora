package org.kazinduzi.rinjora.network.dto;

import com.google.gson.annotations.SerializedName;

/**
 * A single entry in {@code GET /riddles/history} (plan §2.10):
 * {@code { id, riddle, submitted_answer, is_correct, rewarded, attempted_at }}.
 * The {@code riddle} is a {@link RiddleDto} (no {@code answer}).
 */
public class HistoryEntryDto {

    @SerializedName("id")
    private long id;

    @SerializedName("riddle")
    private RiddleDto riddle;

    @SerializedName("submitted_answer")
    private String submittedAnswer;

    @SerializedName("is_correct")
    private boolean correct;

    @SerializedName("rewarded")
    private boolean rewarded;

    @SerializedName("attempted_at")
    private String attemptedAt;

    public long getId() { return id; }
    public RiddleDto getRiddle() { return riddle; }
    public String getSubmittedAnswer() { return submittedAnswer; }
    public boolean isCorrect() { return correct; }
    public boolean isRewarded() { return rewarded; }
    public String getAttemptedAt() { return attemptedAt; }
}
