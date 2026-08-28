package org.kazinduzi.rinjora.data;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import java.io.IOException;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Response;
import org.kazinduzi.rinjora.network.ApiEnvelope;
import org.kazinduzi.rinjora.network.RinjoraApi;
import org.kazinduzi.rinjora.network.RinjoraApiClient;
import org.kazinduzi.rinjora.network.dto.AchievementLibraryDto;

/**
 * Achievements/badges library access (plan §4.4): {@code GET /me/achievements}.
 * Offline caching of the library is deferred to Phase K.
 */
public class RinjoraAchievementsRepository {

    private static final String TAG = "RinjoraAchievementsRepo";

    public interface Callback {
        void onSuccess(AchievementLibraryDto library);

        void onAuthError();

        void onError(String message);
    }

    private final RinjoraApi api;

    public RinjoraAchievementsRepository(Context context) {
        this.api = RinjoraApiClient.get(context).api();
    }

    /** GET /me/achievements — earned count/total plus the full badge library. */
    public void fetch(final RinjoraAchievementsRepository.Callback callback) {
        api.achievements().enqueue(new retrofit2.Callback<ApiEnvelope<AchievementLibraryDto>>() {
            @Override
            public void onResponse(@NonNull Call<ApiEnvelope<AchievementLibraryDto>> call,
                                   @NonNull Response<ApiEnvelope<AchievementLibraryDto>> response) {
                ApiEnvelope<AchievementLibraryDto> envelope = response.body();
                if (response.isSuccessful() && envelope != null && envelope.isSuccess()) {
                    if (callback != null) callback.onSuccess(envelope.getData());
                } else if (response.code() == 401) {
                    if (callback != null) callback.onAuthError();
                } else {
                    if (callback != null) callback.onError(extractError(response, envelope));
                }
            }

            @Override
            public void onFailure(@NonNull Call<ApiEnvelope<AchievementLibraryDto>> call, @NonNull Throwable t) {
                Log.e(TAG, "fetch achievements failed", t);
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
