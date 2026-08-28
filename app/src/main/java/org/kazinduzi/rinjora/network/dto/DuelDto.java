package org.kazinduzi.rinjora.network.dto;

import com.google.gson.annotations.SerializedName;

/**
 * A duel payload from {@code GET /duels} or {@code GET /duels/{id}} (plan §7.1/§7.3).
 * <p>
 * Anti-cheat: {@code riddle.answer} is only non-null if <b>I</b> solved it, and the
 * opponent's {@code submitted_answer} is always null. {@code direction} is provided
 * in the list payload to tell incoming vs outgoing.
 */
public class DuelDto {

    @SerializedName("id")
    private long id;

    @SerializedName("status")
    private String status;

    @SerializedName("wager")
    private int wager;

    @SerializedName("direction")
    private String direction;

    @SerializedName("accepted_at")
    private String acceptedAt;

    @SerializedName("resolved_at")
    private String resolvedAt;

    @SerializedName("riddle")
    private RiddleDto riddle;

    @SerializedName("initiator")
    private DuelUserDto initiator;

    @SerializedName("opponent")
    private DuelUserDto opponent;

    @SerializedName("winner_id")
    private Long winnerId;

    @SerializedName("created_at")
    private String createdAt;

    public long getId() { return id; }
    public String getStatus() { return status; }
    public int getWager() { return wager; }
    public String getDirection() { return direction; }
    public String getAcceptedAt() { return acceptedAt; }
    public String getResolvedAt() { return resolvedAt; }
    public RiddleDto getRiddle() { return riddle; }
    public DuelUserDto getInitiator() { return initiator; }
    public DuelUserDto getOpponent() { return opponent; }
    public Long getWinnerId() { return winnerId; }
    public String getCreatedAt() { return createdAt; }

    /** True when this duel is one I initiated (from list {@code direction}). */
    public boolean isOutgoing() {
        return "outgoing".equalsIgnoreCase(direction);
    }
}
