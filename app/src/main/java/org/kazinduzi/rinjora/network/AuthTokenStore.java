package org.kazinduzi.rinjora.network;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;

import java.io.IOException;
import java.security.GeneralSecurityException;

/**
 * Secure storage for the Rinjora (Kazinduzi) Bearer auth token and the stable
 * {@code device_name} used to keep a single token per device (plan §11).
 *
 * Backed by {@link androidx.security.crypto.EncryptedSharedPreferences}; nothing
 * sensitive is ever persisted in plain ObjectBox.
 */
public final class AuthTokenStore {

    private static final String PREFS_NAME = "rinjora_auth";
    private static final String KEY_TOKEN = "access_token";
    private static final String KEY_TOKEN_EXPIRY = "token_expiry_ms";
    private static final String KEY_DEVICE_NAME = "device_name";
    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_EMAIL = "email";
    private static final String KEY_EMAIL_VERIFICATION_REQUIRED = "email_verification_required";
    private static final String KEY_GUEST_UID = "guest_uid";
    private static final String KEY_IS_GUEST = "is_guest";

    private final SharedPreferences prefs;

    private AuthTokenStore(Context context) {
        SharedPreferences prefs;
        try {
            MasterKey masterKey = new MasterKey.Builder(context)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build();
            prefs = EncryptedSharedPreferences.create(
                    context,
                    PREFS_NAME,
                    masterKey,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM);
        } catch (GeneralSecurityException | IOException e) {
            // Fall back to a plain prefs file only if secure storage is unavailable.
            prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        }
        this.prefs = prefs;
    }

    private static volatile AuthTokenStore instance;

    public static AuthTokenStore get(Context context) {
        if (instance == null) {
            synchronized (AuthTokenStore.class) {
                if (instance == null) {
                    instance = new AuthTokenStore(context.getApplicationContext());
                }
            }
        }
        return instance;
    }

    public void saveToken(String token, Long expiresAtEpochMs) {
        prefs.edit()
                .putString(KEY_TOKEN, token)
                .putLong(KEY_TOKEN_EXPIRY, expiresAtEpochMs == null ? 0L : expiresAtEpochMs)
                .apply();
    }

    /** Saves a fresh bearer token and flags the session as a guest (plan §5 "Guest mode"). */
    public void saveGuestToken(String token, Long expiresAtEpochMs) {
        saveToken(token, expiresAtEpochMs);
        setGuest(true);
    }

    /** True when the current session is a guest (GUEST token, no account). */
    public boolean isGuest() {
        return prefs.getBoolean(KEY_IS_GUEST, false);
    }

    /** Marks the session as guest or account (login conversion flips it to false). */
    public void setGuest(boolean guest) {
        prefs.edit().putBoolean(KEY_IS_GUEST, guest).apply();
    }

    public String getToken() {
        return prefs.getString(KEY_TOKEN, null);
    }

    /** @return true when a token is present and not expired (expiry 0 == no expiry). */
    public boolean hasValidToken() {
        String token = getToken();
        if (token == null || token.isEmpty()) {
            return false;
        }
        long expiry = prefs.getLong(KEY_TOKEN_EXPIRY, 0L);
        return expiry == 0L || expiry > System.currentTimeMillis();
    }

    public long getTokenExpiry() {
        return prefs.getLong(KEY_TOKEN_EXPIRY, 0L);
    }

    public void saveDeviceName(String deviceName) {
        prefs.edit().putString(KEY_DEVICE_NAME, deviceName).apply();
    }

    /**
     * Stable per-install device name. Reused across logins so the server keeps a
     * single active token per device (plan §11): {@code Android_<installId>}.
     */
    public String getOrCreateDeviceName(Context context) {
        String existing = prefs.getString(KEY_DEVICE_NAME, null);
        if (existing != null) {
            return existing;
        }
        String androidId = android.provider.Settings.Secure.getString(
                context.getContentResolver(),
                android.provider.Settings.Secure.ANDROID_ID);
        String deviceName = "Android_" + (androidId == null
                ? Long.toString(System.currentTimeMillis())
                : androidId);
        saveDeviceName(deviceName);
        return deviceName;
    }

    public String getGuestUid() {
        return prefs.getString(KEY_GUEST_UID, null);
    }

    public void saveGuestUid(String guestUid) {
        prefs.edit().putString(KEY_GUEST_UID, guestUid).apply();
    }

    /**
     * Stable per-install guest id (plan §5 "Guest mode"). Reused across 401/guest
     * re-sessions so the server returns the same player with a fresh token; derived
     * from {@code ANDROID_ID} so reinstalls keep the same identity when the OS id
     * survives. {@code 9774d56d682e549c} is the emulator's constant bogus id.
     */
    public String getOrCreateGuestUid(Context context) {
        String existing = prefs.getString(KEY_GUEST_UID, null);
        if (existing != null) {
            return existing;
        }
        String androidId = android.provider.Settings.Secure.getString(
                context.getContentResolver(),
                android.provider.Settings.Secure.ANDROID_ID);
        String guestUid;
        if (androidId != null && !androidId.isEmpty()
                && !"9774d56d682e549c".equals(androidId)) {
            guestUid = "Guest_" + androidId;
        } else {
            guestUid = "Guest_" + java.util.UUID.randomUUID().toString();
        }
        saveGuestUid(guestUid);
        return guestUid;
    }

    public void saveUserId(long userId) {
        prefs.edit().putLong(KEY_USER_ID, userId).apply();
    }

    public long getUserId() {
        return prefs.getLong(KEY_USER_ID, -1L);
    }

    /**
     * Last-known account email. Kept so the Enter Code screen can prefill it when
     * a 403 "email not verified" throws the user back to verification.
     */
    public void saveEmail(String email) {
        prefs.edit().putString(KEY_EMAIL, email).apply();
    }

    public String getEmail() {
        return prefs.getString(KEY_EMAIL, null);
    }

    /**
     * True when the last auth failure was a 403 "Your email address is not
     * verified." — the auth host should open on the Enter Code screen, not login.
     */
    public void setEmailVerificationRequired(boolean required) {
        prefs.edit().putBoolean(KEY_EMAIL_VERIFICATION_REQUIRED, required).apply();
    }

    public boolean isEmailVerificationRequired() {
        return prefs.getBoolean(KEY_EMAIL_VERIFICATION_REQUIRED, false);
    }

    public void clear() {
        prefs.edit().clear().apply();
    }
}
