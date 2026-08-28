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

    public void saveUserId(long userId) {
        prefs.edit().putLong(KEY_USER_ID, userId).apply();
    }

    public long getUserId() {
        return prefs.getLong(KEY_USER_ID, -1L);
    }

    public void clear() {
        prefs.edit().clear().apply();
    }
}
