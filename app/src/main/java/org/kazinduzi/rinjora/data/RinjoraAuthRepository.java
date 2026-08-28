package org.kazinduzi.rinjora.data;

import android.content.Context;

import androidx.annotation.NonNull;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import org.kazinduzi.rinjora.network.ApiEnvelope;
import org.kazinduzi.rinjora.network.AuthTokenStore;
import org.kazinduzi.rinjora.network.RinjoraApi;
import org.kazinduzi.rinjora.network.RinjoraApiClient;
import org.kazinduzi.rinjora.network.dto.LoginResponseDto;
import org.kazinduzi.rinjora.network.dto.UserDto;

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

    private final Context context;
    private final RinjoraApi api;

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

        api.register(body).enqueue(new Callback<ApiEnvelope<Void>>() {
            @Override
            public void onResponse(@NonNull Call<ApiEnvelope<Void>> call,
                                   @NonNull Response<ApiEnvelope<Void>> response) {
                ApiEnvelope<Void> envelope = response.body();
                if (response.isSuccessful() && envelope != null && envelope.isSuccess()) {
                    String message = envelope.getMessage();
                    // Plan §1.1: in production a 6-digit code is emailed (verified account
                    // flow is a later step). Registration itself is the success here.
                    callback.onSuccess();
                } else {
                    callback.onError(extractError(response, envelope));
                }
            }

            @Override
            public void onFailure(@NonNull Call<ApiEnvelope<Void>> call, @NonNull Throwable t) {
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
                callback.onError(t.getMessage() == null ? "Network error" : t.getMessage());
            }
        });
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
