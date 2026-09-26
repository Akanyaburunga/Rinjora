package org.kazinduzi.rinjora.data;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import org.kazinduzi.rinjora.network.ApiEnvelope;
import org.kazinduzi.rinjora.network.AuthTokenStore;
import org.kazinduzi.rinjora.network.RinjoraApi;
import org.kazinduzi.rinjora.network.RinjoraApiClient;
import org.kazinduzi.rinjora.network.dto.GuestSessionDto;
import org.kazinduzi.rinjora.network.dto.LoginResponseDto;
import org.kazinduzi.rinjora.network.dto.UserDto;
import org.kazinduzi.rinjora.util.KirundiUi;

/**
 * Auth operations against the Rinjora (Kazinduzi) backend (plan §1).
 *
 * On success the {@link AuthTokenStore} is updated with the Bearer token and the
 * stable {@code device_name}, so every subsequent Retrofit call is authenticated.
 */
public class RinjoraAuthRepository {

    public interface AuthCallback {
        void onSuccess();

        void onError(String message);
    }

    public interface UserCallback {
        void onUser(UserDto user);

        void onError(String message);
    }

    /**
     * Distinguishes the backend's verified-email outcomes (docs
     * {@code mobile-api-email-verification.md}), because the client must react to
     * a wrong code ("Igiharuro si ibyo") differently from an expired one (resend).
     */
    public interface VerifyEmailCallback {
        void onSuccess();

        void onInvalidCode();

        void onExpiredCode();

        void onError(String message);
    }

    public interface ResendCallback {
        void onSuccess(String message);

        void onError(String message);
    }

    private static final String TAG = "RinjoraAuthRepository";

    private final Context context;
    private final RinjoraApi api;

    /**
     * Single-flight guard: concurrent {@link #ensureGuest} calls (several resumed
     * fragments, a tap during background provisioning) collapse into one mint so
     * the throttled {@code /auth/guest} endpoint is never hit in parallel.
     */
    private static final Object GUEST_LOCK = new Object();
    private static boolean guestInFlight;
    private static final List<AuthCallback> guestWaiters = new ArrayList<>();

    public RinjoraAuthRepository(Context context) {
        this.context = context.getApplicationContext();
        this.api = RinjoraApiClient.get(context).api();
    }

    public void register(@NonNull String name, @NonNull String email,
                         @NonNull String password, @NonNull String passwordConfirmation,
                         final AuthCallback callback) {
        Map<String, Object> body = new HashMap<>();
        body.put("name", name);
        body.put("email", email);
        body.put("password", password);
        body.put("password_confirmation", passwordConfirmation);
        // Cap conversion: a guest who registers attaches their stored uid so the
        // server moves their rounds/attempts onto the account (plan §5 "Guest mode").
        AuthTokenStore store = AuthTokenStore.get(context);
        if (store.isGuest()) {
            body.put("guest_uid", store.getOrCreateGuestUid(context));
        }

        api.register(body).enqueue(new Callback<ApiEnvelope<Void>>() {
            @Override
            public void onResponse(@NonNull Call<ApiEnvelope<Void>> call,
                                   @NonNull Response<ApiEnvelope<Void>> response) {
                ApiEnvelope<Void> envelope = response.body();
                if (response.isSuccessful() && envelope != null && envelope.isSuccess()) {
                    // Registration is complete: a 6-digit code has been dispatched —
                    // emailed in staging/prod, or written to the backend log in local
                    // dev (docs mobile-api-email-verification.md §4). Never surface an
                    // "email unreachable" error here; the code reaches the user somehow.
                    callback.onSuccess();
                } else {
                    callback.onError(extractError(response, envelope));
                }
            }

            @Override
            public void onFailure(@NonNull Call<ApiEnvelope<Void>> call, @NonNull Throwable t) {
                if (call.isCanceled()) {
                    Log.d(TAG, "auth request canceled");
                    return;
                }
                Log.e(TAG, "auth request failed", t);
                callback.onError(t.getMessage() == null ? "Network error" : t.getMessage());
            }
        });
    }

    /**
     * Submits the emailed 6-digit code. Public endpoint (no token required), so a
     * freshly registered user can complete the flow before logging in.
     * Throttled {@code 10/min} server-side.
     */
    public void verifyEmail(@NonNull String email, @NonNull String verificationCode,
                            final VerifyEmailCallback callback) {
        Map<String, Object> body = new HashMap<>();
        body.put("email", email);
        body.put("verification_code", verificationCode);

        api.verifyEmail(body).enqueue(new Callback<ApiEnvelope<Void>>() {
            @Override
            public void onResponse(@NonNull Call<ApiEnvelope<Void>> call,
                                   @NonNull Response<ApiEnvelope<Void>> response) {
                ApiEnvelope<Void> envelope = response.body();
                if (response.isSuccessful() && envelope != null && envelope.isSuccess()) {
                    callback.onSuccess();
                } else {
                    String message = extractError(response, envelope);
                    // 200 "Email already verified." and 400 invalid/expired all carry a message.
                    if (response.code() == 400 && message.toLowerCase().contains("expired")) {
                        callback.onExpiredCode();
                    } else if (response.code() == 400 || message.toLowerCase().contains("invalid")) {
                        callback.onInvalidCode();
                    } else {
                        callback.onError(message);
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call<ApiEnvelope<Void>> call, @NonNull Throwable t) {
                if (call.isCanceled()) {
                    Log.d(TAG, "auth request canceled");
                    return;
                }
                Log.e(TAG, "auth request failed", t);
                callback.onError(t.getMessage() == null ? "Network error" : t.getMessage());
            }
        });
    }

    /**
     * Sends a fresh 6-digit code (10-minute expiry). Public endpoint, throttled
     * {@code 3/min} server-side. 200 "Email already verified." is a pass state.
     */
    public void resendVerificationCode(@NonNull String email,
                                       final ResendCallback callback) {
        Map<String, Object> body = new HashMap<>();
        body.put("email", email);

        api.resendVerificationCode(body).enqueue(new Callback<ApiEnvelope<Void>>() {
            @Override
            public void onResponse(@NonNull Call<ApiEnvelope<Void>> call,
                                   @NonNull Response<ApiEnvelope<Void>> response) {
                ApiEnvelope<Void> envelope = response.body();
                if (response.isSuccessful() && envelope != null && envelope.isSuccess()) {
                    callback.onSuccess(envelope.getMessage() == null ? "" : envelope.getMessage());
                } else {
                    callback.onError(extractError(response, envelope));
                }
            }

            @Override
            public void onFailure(@NonNull Call<ApiEnvelope<Void>> call, @NonNull Throwable t) {
                if (call.isCanceled()) {
                    Log.d(TAG, "auth request canceled");
                    return;
                }
                Log.e(TAG, "auth request failed", t);
                callback.onError(t.getMessage() == null ? "Network error" : t.getMessage());
            }
        });
    }

    public void login(@NonNull String email, @NonNull String password,
                      final AuthCallback callback) {
        Map<String, Object> body = new HashMap<>();
        body.put("email", email);
        body.put("password", password);
        body.put("device_name", AuthTokenStore.get(context).getOrCreateDeviceName(context));

        api.login(body).enqueue(new Callback<ApiEnvelope<LoginResponseDto>>() {
            @Override
            public void onResponse(@NonNull Call<ApiEnvelope<LoginResponseDto>> call,
                                   @NonNull Response<ApiEnvelope<LoginResponseDto>> response) {
                ApiEnvelope<LoginResponseDto> envelope = response.body();
                if (response.isSuccessful() && envelope != null && envelope.isSuccess()
                        && envelope.getData() != null) {
                    LoginResponseDto data = envelope.getData();
                    if (data.getToken() == null || data.getToken().isEmpty()) {
                        callback.onError("No token returned");
                        return;
                    }
                    // Persist the credential securely.
                    AuthTokenStore store = AuthTokenStore.get(context);
                    Long expiresAt = parseExpiry(data.getExpiresAt());
                    store.saveToken(data.getToken(), expiresAt);
                    store.saveEmail(email);
                    // A real login ends any guest session; the GuestApp token is
                    // overwritten by the account token (plan §5 conversion).
                    store.setGuest(false);
                    if (data.getUser() != null) {
                        store.saveUserId(data.getUser().getId());
                    }
                    callback.onSuccess();
                } else {
                    callback.onError(extractError(response, envelope));
                }
            }

            @Override
            public void onFailure(@NonNull Call<ApiEnvelope<LoginResponseDto>> call,
                                  @NonNull Throwable t) {
                if (call.isCanceled()) {
                    Log.d(TAG, "auth request canceled");
                    return;
                }
                Log.e(TAG, "auth request failed", t);
                callback.onError(t.getMessage() == null ? "Network error" : t.getMessage());
            }
        });
    }

    /**
     * Creates (or refreshes) a guest session (plan §5): {@code POST /api/auth/guest}
     * with the stable {@code guest_uid}. The same uid always resolves to the same
     * player server-side, and any prior {@code GuestApp} token is revoked.
     */
    public void guest(@NonNull String guestUid, final AuthCallback callback) {
        Map<String, Object> body = new HashMap<>();
        body.put("guest_uid", guestUid);

        AuthTokenStore store = AuthTokenStore.get(context);
        store.saveGuestUid(guestUid);

        api.guest(body).enqueue(new Callback<ApiEnvelope<GuestSessionDto>>() {
            @Override
            public void onResponse(@NonNull Call<ApiEnvelope<GuestSessionDto>> call,
                                   @NonNull Response<ApiEnvelope<GuestSessionDto>> response) {
                ApiEnvelope<GuestSessionDto> envelope = response.body();
                if (response.isSuccessful() && envelope != null && envelope.isSuccess()
                        && envelope.getData() != null) {
                    GuestSessionDto data = envelope.getData();
                    if (data.getToken() == null || data.getToken().isEmpty()) {
                        callback.onError("No token returned");
                        return;
                    }
                    store.saveGuestToken(data.getToken(), parseExpiry(data.getExpiresAt()));
                    if (data.getUser() != null) {
                        store.saveUserId(data.getUser().getId());
                    }
                    callback.onSuccess();
                } else if (response.code() == 429) {
                    callback.onError(KirundiUi.G_THROTTLE);
                } else {
                    callback.onError(extractError(response, envelope));
                }
            }

            @Override
            public void onFailure(@NonNull Call<ApiEnvelope<GuestSessionDto>> call, @NonNull Throwable t) {
                if (call.isCanceled()) {
                    Log.d(TAG, "auth request canceled");
                    return;
                }
                Log.e(TAG, "auth request failed", t);
                callback.onError(t.getMessage() == null ? "Network error" : t.getMessage());
            }
        });
    }

    /**
     * Guarantees a usable session before gameplay (plan §6 auth guard): a stored
     * valid token (guest or account) passes through untouched; otherwise it mints a
     * guest session from the stored/derived {@code guest_uid} instead of showing a
     * login wall.
     */
    public void ensureGuest(final AuthCallback callback) {
        AuthTokenStore store = AuthTokenStore.get(context);
        if (store.hasValidToken()) {
            callback.onSuccess();
            return;
        }
        synchronized (GUEST_LOCK) {
            if (guestInFlight) {
                guestWaiters.add(callback);
                return;
            }
            guestInFlight = true;
            // The initiator is notified through the same drain as followers, otherwise
            // its onSuccess/onError is dropped and the caller hangs forever.
            guestWaiters.add(callback);
        }
        guest(store.getOrCreateGuestUid(context), new AuthCallback() {
            @Override
            public void onSuccess() {
                drainGuest(true, null);
            }

            @Override
            public void onError(String message) {
                drainGuest(false, message);
            }
        });
    }

    /** Notifies every caller that coalesced onto the single in-flight mint. */
    private void drainGuest(boolean success, String message) {
        List<AuthCallback> waiters;
        synchronized (GUEST_LOCK) {
            guestInFlight = false;
            waiters = new ArrayList<>(guestWaiters);
            guestWaiters.clear();
        }
        for (AuthCallback waiter : waiters) {
            if (success) {
                waiter.onSuccess();
            } else {
                waiter.onError(message);
            }
        }
    }

    public void currentUser(final UserCallback callback) {
        api.currentUser().enqueue(new Callback<ApiEnvelope<UserDto>>() {
            @Override
            public void onResponse(@NonNull Call<ApiEnvelope<UserDto>> call,
                                   @NonNull Response<ApiEnvelope<UserDto>> response) {
                ApiEnvelope<UserDto> envelope = response.body();
                if (response.isSuccessful() && envelope != null && envelope.isSuccess()
                        && envelope.getData() != null) {
                    callback.onUser(envelope.getData());
                } else {
                    callback.onError(extractError(response, envelope));
                }
            }

            @Override
            public void onFailure(@NonNull Call<ApiEnvelope<UserDto>> call, @NonNull Throwable t) {
                if (call.isCanceled()) {
                    Log.d(TAG, "auth request canceled");
                    return;
                }
                Log.e(TAG, "auth request failed", t);
                callback.onError(t.getMessage() == null ? "Network error" : t.getMessage());
            }
        });
    }

    public void logout(final AuthCallback callback) {
        Map<String, Object> body = new HashMap<>();
        body.put("device_name", AuthTokenStore.get(context).getOrCreateDeviceName(context));

        api.logout(body).enqueue(new Callback<ApiEnvelope<Void>>() {
            @Override
            public void onResponse(@NonNull Call<ApiEnvelope<Void>> call,
                                   @NonNull Response<ApiEnvelope<Void>> response) {
                // Always clear locally regardless of response; plan §1.5 revokes then clears.
                AuthTokenStore.get(context).clear();
                callback.onSuccess();
            }

            @Override
            public void onFailure(@NonNull Call<ApiEnvelope<Void>> call, @NonNull Throwable t) {
                Log.e(TAG, "logout failed (best-effort clear)", t);
                AuthTokenStore.get(context).clear();
                callback.onSuccess();
            }
        });
    }

    /** Best-effort ISO-8601 parsing (minSdk 23 compatible). Returns null when absent/unparseable (== no expiry). */
    private Long parseExpiry(String iso) {
        if (iso == null || iso.isEmpty()) {
            return null;
        }
        // Server timestamps are ISO-8601, usually UTC ("...Z"). Parse ages API 23+.
        try {
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat(
                    "yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.US);
            sdf.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));
            return sdf.parse(iso).getTime();
        } catch (Exception e) {
            return null;
        }
    }

    /** Derives a human message from an envelope or HTTP-style error body. */
    private String extractError(Response<?> response, ApiEnvelope<?> envelope) {
        String message = "Request failed";
        if (envelope != null && envelope.getMessage() != null && !envelope.getMessage().isEmpty()) {
            message = envelope.getMessage();
        } else if (response.errorBody() != null) {
            try {
                String raw = response.errorBody().string();
                // Best-effort: many backends return { "message": "..." } or { "errors": {...} }
                com.google.gson.JsonObject obj = com.google.gson.JsonParser.parseString(raw)
                        .getAsJsonObject();
                // Prefer a concrete field error (e.g. 422 "email has already been taken")
                // over the generic Laravel message.
                if (obj.has("errors") && obj.get("errors").isJsonObject()) {
                    com.google.gson.JsonObject errors = obj.getAsJsonObject("errors");
                    for (String key : errors.keySet()) {
                        com.google.gson.JsonElement first = errors.get(key);
                        if (first != null && first.isJsonArray() && first.getAsJsonArray().size() > 0) {
                            String field = first.getAsJsonArray().get(0).getAsString();
                            if (field != null && !field.isEmpty()) {
                                return field;
                            }
                        }
                    }
                }
                if (obj.has("message")) {
                    message = obj.get("message").getAsString();
                }
            } catch (IOException | RuntimeException ignored) {
                // fall through to the default message
            }
        }
        return message;
    }
}
