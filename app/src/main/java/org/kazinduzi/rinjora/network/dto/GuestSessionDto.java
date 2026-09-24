package org.kazinduzi.rinjora.network.dto;

import com.google.gson.JsonElement;
import com.google.gson.annotations.SerializedName;

/**
 * Payload of {@code POST /api/auth/guest} (plan §5): the guest {@code user} plus a
 * fresh {@code token} / {@code token_type} / {@code expires_at}, exactly like
 * {@link LoginResponseDto} with an extra {@code guest} marker for the session.
 */
public class GuestSessionDto {

    @SerializedName("user")
    private UserDto user;

    @SerializedName("token")
    private String token;

    @SerializedName("token_type")
    private String tokenType;

    @SerializedName("expires_at")
    private String expiresAt;

    /** Backend shape of {@code guest} is not a plain boolean (observed as an array);
     *  kept as JSON so Gson never fails on it. Not consumed by any logic. */
    @SerializedName("guest")
    private JsonElement guest;

    public UserDto getUser() { return user; }
    public String getToken() { return token; }
    public String getTokenType() { return tokenType; }

    /** ISO-8601, null when the token has no expiry. */
    public String getExpiresAt() { return expiresAt; }

    public JsonElement getGuest() { return guest; }
}