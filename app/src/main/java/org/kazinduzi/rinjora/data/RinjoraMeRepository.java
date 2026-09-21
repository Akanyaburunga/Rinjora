package org.kazinduzi.rinjora.data;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Response;
import org.kazinduzi.rinjora.network.ApiEnvelope;
import org.kazinduzi.rinjora.network.RinjoraApi;
import org.kazinduzi.rinjora.network.RinjoraApiClient;
import org.kazinduzi.rinjora.network.dto.MeDto;

import java.io.IOException;

/**
 * Thin, strictly-online repository for the live round-backed profile
 * {@code GET /api/me} (parity plan §5). The Jewe tab shows only server numbers —
 * nothing is cached locally, so the profile always reflects the logged-in user.
 */
public class RinjoraMeRepository {

    private static final String TAG = "RinjoraMeRepository";

    public interface Callback {
        void onSuccess(MeDto me);

        void onAuthError();

        void onError(String message);
    }

    private final RinjoraApi api;

    public RinjoraMeRepository(Context context) {
        this.api = RinjoraApiClient.get(context).api();
    }

    public void fetch(final Callback callback) {
        api.me().enqueue(new retrofit2.Callback<ApiEnvelope<MeDto>>() {
            @Override
            public void onResponse(@NonNull Call<ApiEnvelope<MeDto>> call,
                                   @NonNull Response<ApiEnvelope<MeDto>> response) {
                ApiEnvelope<MeDto> envelope = response.body();
                if (response.isSuccessful() && envelope != null && envelope.isSuccess()
                        && envelope.getData() != null) {
                    if (callback != null) callback.onSuccess(envelope.getData());
                } else if (response.code() == 401) {
                    if (callback != null) callback.onAuthError();
                } else {
                    if (callback != null) callback.onError(message(response, envelope));
                }
            }

            @Override
            public void onFailure(@NonNull Call<ApiEnvelope<MeDto>> call, @NonNull Throwable t) {
                Log.e(TAG, "fetch failed", t);
                if (callback != null) callback.onError(t.getMessage() == null ? "Network error" : t.getMessage());
            }
        });
    }

    private String message(Response<?> response, ApiEnvelope<?> envelope) {
        if (envelope != null && envelope.getMessage() != null && !envelope.getMessage().isEmpty()) {
            return envelope.getMessage();
        }
        ResponseBody body = response.errorBody();
        if (body != null) {
            try {
                String raw = body.string();
                com.google.gson.JsonObject obj = com.google.gson.JsonParser.parseString(raw).getAsJsonObject();
                if (obj.has("message")) {
                    return obj.get("message").getAsString();
                }
            } catch (IOException | RuntimeException ignored) {
                // fall through
            }
        }
        return "Request failed (HTTP " + response.code() + ")";
    }
}