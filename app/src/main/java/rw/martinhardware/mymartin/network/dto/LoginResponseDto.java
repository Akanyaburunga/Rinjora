package rw.martinhardware.mymartin.network.dto;

import com.google.gson.annotations.SerializedName;

/**
 * Login response payload (plan §1.4): the authenticated {@code user} plus the
 * {@code token} / {@code token_type} / {@code expires_at}.
 */
public class LoginResponseDto {

    @SerializedName("user")
    private UserDto user;

    @SerializedName("token")
    private String token;

    @SerializedName("token_type")
    private String tokenType;

    @SerializedName("expires_at")
    private String expiresAt;

    public UserDto getUser() {
        return user;
    }

    public String getToken() {
        return token;
    }

    public String getTokenType() {
        return tokenType;
    }

    /** ISO-8601, null when the token has no expiry. */
    public String getExpiresAt() {
        return expiresAt;
    }
}
