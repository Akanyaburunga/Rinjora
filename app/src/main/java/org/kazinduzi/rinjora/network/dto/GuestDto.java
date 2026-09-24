package org.kazinduzi.rinjora.network.dto;

import com.google.gson.annotations.SerializedName;

/**
 * Per-mode guest cap carried on round-start responses (plan §5): {@code { mode,
 * limit, used, remaining, requires_registration }}. The backend enforces the cap;
 * this block lets the client show how many free plays are left and detect when an
 * account is required before a round starts.
 */
public class GuestDto {

    @SerializedName("mode")
    private String mode;

    @SerializedName("limit")
    private int limit;

    @SerializedName("used")
    private int used;

    @SerializedName("remaining")
    private int remaining;

    @SerializedName("requires_registration")
    private boolean requiresRegistration;

    public String getMode() { return mode; }
    public int getLimit() { return limit; }
    public int getUsed() { return used; }
    public int getRemaining() { return remaining; }
    public boolean isRequiresRegistration() { return requiresRegistration; }
}