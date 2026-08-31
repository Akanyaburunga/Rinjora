package org.kazinduzi.rinjora.network.dto;

import com.google.gson.annotations.SerializedName;

/**
 * A single Heraheza attempt in {@code GET /proverbs/history} (parity plan §4.1):
 * {@code { id, proverb, submitted_answer, is_correct, rewarded, attempted_at }}.
 * The {@code proverb} is a {@link ProverbDto} (no {@code answer} until solved/revealed).
 */
public class ProverbHistoryEntryDto {

    @SerializedName("id")
    private long id;

    @SerializedName("proverb")
    private ProverbDto proverb;

    @SerializedName("submitted_answer")
    private String submittedAnswer;

    @SerializedName("is_correct")
    private boolean correct;

    @SerializedName("rewarded")
    private boolean rewarded;

    @SerializedName("attempted_at")
    private String attemptedAt;

    public long getId() { return id; }
    public ProverbDto getProverb() { return proverb; }
    public String getSubmittedAnswer() { return submittedAnswer; }
    public boolean isCorrect() { return correct; }
    public boolean isRewarded() { return rewarded; }
    public String getAttemptedAt() { return attemptedAt; }
}
