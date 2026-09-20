package org.kazinduzi.rinjora.network.dto;

import com.google.gson.annotations.SerializedName;

/**
 * Payload of {@code POST /api/contributions} (parity plan §4.8):
 * {@code { status: "pending" }}. The backend routes the record to the matching
 * submission table by {@code type}.
 */
public class ContributionResponseDto {

    @SerializedName("status")
    private String status;

    public String getStatus() { return status; }
}