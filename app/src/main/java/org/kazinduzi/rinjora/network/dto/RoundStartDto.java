package org.kazinduzi.rinjora.network.dto;

import com.google.gson.annotations.SerializedName;

/**
 * Payload of {@code POST /api/games/{mode}/rounds}: {@code { round, item }} with the
 * first item of the fresh round. The item never contains the answer.
 */
public class RoundStartDto {

    @SerializedName("round")
    private RoundDto round;

    @SerializedName("item")
    private RoundItemDto item;

    public RoundDto getRound() { return round; }
    public RoundItemDto getItem() { return item; }
}