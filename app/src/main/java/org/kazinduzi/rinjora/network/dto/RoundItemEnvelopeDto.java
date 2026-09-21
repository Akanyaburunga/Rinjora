package org.kazinduzi.rinjora.network.dto;

import com.google.gson.annotations.SerializedName;

/**
 * Payload of {@code GET /api/games/{mode}/rounds/{round}/items/{position}}
 * (parity plan §5): the item is served nested under the {@code item} key:
 * {@code { success, data: { item: {...} } }}. The per-position answered state
 * (Back nav, G-1) is carried on the wrapped {@link RoundItemDto}.
 */
public class RoundItemEnvelopeDto {

    @SerializedName("item")
    private RoundItemDto item;

    public RoundItemDto getItem() { return item; }
}