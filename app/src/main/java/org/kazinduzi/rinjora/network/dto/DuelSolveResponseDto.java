package org.kazinduzi.rinjora.network.dto;

import com.google.gson.annotations.SerializedName;

/**
 * Body of {@code POST /duels/{id}/solve} (plan §7.6): reveals correctness to the
 * solver only. {@code answer} is only non-null if correct (revealed to me only);
 * {@code resolved} true when the duel is over (both solved / faster wins / settle).
 */
public class DuelSolveResponseDto {

    @SerializedName("correct")
    private Boolean correct;

    @SerializedName("resolved")
    private boolean resolved;

    @SerializedName("answer")
    private String answer;

    @SerializedName("message")
    private String message;

    public Boolean getCorrect() { return correct; }
    public boolean isResolved() { return resolved; }
    public String getAnswer() { return answer; }
    public String getMessage() { return message; }
}
