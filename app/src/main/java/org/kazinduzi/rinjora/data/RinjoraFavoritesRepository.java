package org.kazinduzi.rinjora.data;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import java.io.IOException;
import java.util.List;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Response;
import org.kazinduzi.rinjora.network.ApiEnvelope;
import org.kazinduzi.rinjora.network.RinjoraApi;
import org.kazinduzi.rinjora.network.RinjoraApiClient;
import org.kazinduzi.rinjora.network.dto.RiddleDto;

/**
 * Favorites list access (plan §6.1): {@code GET /me/favorites}. Removal from the
 * list reuses {@link RinjoraRiddleRepository#setFavorite(long, boolean, ...)}.
 * Offline caching of the list is deferred to Phase K.
 */
public class RinjoraFavoritesRepository {

    private static final String TAG = "RinjoraFavoritesRepo";

    public interface Callback {
        void onSuccess(List<RiddleDto> favorites);

        void onAuthError();

        void onError(String message);
    }

    private final RinjoraApi api;

    public RinjoraFavoritesRepository(Context context) {
        this.api = RinjoraApiClient.get(context).api();
    }

    /** GET /me/favorites — a list of solved-marked riddle payloads. */
    public void fetch(final RinjoraFavoritesRepository.Callback callback) {
        api.favorites().enqueue(new retrofit2.Callback<ApiEnvelope<List<RiddleDto>>>() {
            @Override
            public void onResponse(@NonNull Call<ApiEnvelope<List<RiddleDto>>> call,
                                   @NonNull Response<ApiEnvelope<List<RiddleDto>>> response) {
                ApiEnvelope<List<RiddleDto>> envelope = response.body();
                if (response.isSuccessful() && envelope != null && envelope.isSuccess()) {
                    if (callback != null) callback.onSuccess(envelope.getData());
                } else if (response.code() == 401) {
                    if (callback != null) callback.onAuthError();
                } else {
                    if (callback != null) callback.onError(extractError(response, envelope));
                }
            }

            @Override
            public void onFailure(@NonNull Call<ApiEnvelope<List<RiddleDto>>> call, @NonNull Throwable t) {
                Log.e(TAG, "fetch favorites failed", t);
                if (callback != null) callback.onError(t.getMessage() == null ? "Network error" : t.getMessage());
            }
        });
    }

    private String extractError(Response<?> response, ApiEnvelope<?> envelope) {
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
