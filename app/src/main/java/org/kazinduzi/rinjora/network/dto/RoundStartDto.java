package org.kazinduzi.rinjora.network.dto;

import com.google.gson.annotations.SerializedName;

/**
 * Payload of {@code POST /api/games/{mode}/rounds}: {@code { round, item, guest? }}
 * with the first item of the fresh round. The item never contains the answer.
 */
public class RoundStartDto {

    @SerializedName("round")
    private RoundDto round;

    @SerializedName("item")
    private RoundItemDto item;

    /** Per-mode guest cap block; present only while playing without an account. */
    @SerializedName("guest")
    private GuestDto guest;

    public RoundDto getRound() { return round; }
    public RoundItemDto getItem() { return item; }
    public GuestDto getGuest() { return guest; }
}