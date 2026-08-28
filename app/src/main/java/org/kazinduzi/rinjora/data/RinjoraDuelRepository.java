package org.kazinduzi.rinjora.data;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Response;
import org.kazinduzi.rinjora.network.ApiEnvelope;
import org.kazinduzi.rinjora.network.RinjoraApi;
import org.kazinduzi.rinjora.network.RinjoraApiClient;
import org.kazinduzi.rinjora.network.dto.DuelDto;
import org.kazinduzi.rinjora.network.dto.DuelSolveResponseDto;

/**
 * Duels (PvP) access (plan §7.1–§7.6): list/details, create, accept/decline, and
 * solve. Business {@code 422}s (e.g. wager beyond reputation, duplicated pending
 * duel) are surfaced as regular error messages.
 */
public class RinjoraDuelRepository {

    private static final String TAG = "RinjoraDuelRepo";

    public interface Callback<T> {
        void onSuccess(T data);

        void onAuthError();

        void onError(String message);
    }

    private final RinjoraApi api;

    public RinjoraDuelRepository(Context context) {
        this.api = RinjoraApiClient.get(context).api();
    }

    /** GET /duels — all duels I am party to (incoming/outgoing). */
    public void fetchDuels(final Callback<List<DuelDto>> callback) {
        api.duels().enqueue(new retrofit2.Callback<ApiEnvelope<List<DuelDto>>>() {
            @Override
            public void onResponse(@NonNull Call<ApiEnvelope<List<DuelDto>>> call,
                                   @NonNull Response<ApiEnvelope<List<DuelDto>>> response) {
                ApiEnvelope<List<DuelDto>> envelope = response.body();
                if (response.isSuccessful() && envelope != null && envelope.isSuccess()) {
                    if (callback != null) callback.onSuccess(envelope.getData());
                } else if (response.code() == 401) {
                    if (callback != null) callback.onAuthError();
                } else {
                    if (callback != null) callback.onError(extractError(response, envelope));
                }
            }

            @Override
            public void onFailure(@NonNull Call<ApiEnvelope<List<DuelDto>>> call, @NonNull Throwable t) {
                Log.e(TAG, "fetchDuels failed", t);
                if (callback != null) callback.onError(msg(t));
            }
        });
    }

    /** GET /duels/{id} — live status for a participant. */
    public void fetchDuel(final long id, final Callback<DuelDto> callback) {
        api.duel(id).enqueue(new retrofit2.Callback<ApiEnvelope<DuelDto>>() {
            @Override
            public void onResponse(@NonNull Call<ApiEnvelope<DuelDto>> call,
                                   @NonNull Response<ApiEnvelope<DuelDto>> response) {
                ApiEnvelope<DuelDto> envelope = response.body();
                if (response.isSuccessful() && envelope != null && envelope.isSuccess()) {
                    if (callback != null) callback.onSuccess(envelope.getData());
                } else if (response.code() == 401) {
                    if (callback != null) callback.onAuthError();
                } else {
                    if (callback != null) callback.onError(extractError(response, envelope));
                }
            }

            @Override
            public void onFailure(@NonNull Call<ApiEnvelope<DuelDto>> call, @NonNull Throwable t) {
                Log.e(TAG, "fetchDuel failed", t);
                if (callback != null) callback.onError(msg(t));
            }
        });
    }

    /** POST /duels — create a challenge (plan §7.2). */
    public void create(long opponentId, long riddleId, int wager, final Callback<DuelDto> callback) {
        Map<String, Object> body = new HashMap<>();
        body.put("opponent_id", opponentId);
        body.put("riddle_id", riddleId);
        body.put("wager", wager);

        api.createDuel(body).enqueue(new retrofit2.Callback<ApiEnvelope<DuelDto>>() {
            @Override
            public void onResponse(@NonNull Call<ApiEnvelope<DuelDto>> call,
                                   @NonNull Response<ApiEnvelope<DuelDto>> response) {
                ApiEnvelope<DuelDto> envelope = response.body();
                if (response.isSuccessful() && envelope != null && envelope.isSuccess()) {
                    if (callback != null) callback.onSuccess(envelope.getData());
                } else if (response.code() == 401) {
                    if (callback != null) callback.onAuthError();
                } else {
                    if (callback != null) callback.onError(extractError(response, envelope));
                }
            }

            @Override
            public void onFailure(@NonNull Call<ApiEnvelope<DuelDto>> call, @NonNull Throwable t) {
                Log.e(TAG, "create failed", t);
                if (callback != null) callback.onError(msg(t));
            }
        });
    }

    /** POST /duels/{id}/accept — opponent accepts (plan §7.4). */
    public void accept(final long id, final Callback<DuelDto> callback) {
        api.acceptDuel(id).enqueue(new retrofit2.Callback<ApiEnvelope<DuelDto>>() {
            @Override
            public void onResponse(@NonNull Call<ApiEnvelope<DuelDto>> call,
                                   @NonNull Response<ApiEnvelope<DuelDto>> response) {
                ApiEnvelope<DuelDto> envelope = response.body();
                if (response.isSuccessful() && envelope != null && envelope.isSuccess()) {
                    if (callback != null) callback.onSuccess(envelope.getData());
                } else if (response.code() == 401) {
                    if (callback != null) callback.onAuthError();
                } else {
                    if (callback != null) callback.onError(extractError(response, envelope));
                }
            }

            @Override
            public void onFailure(@NonNull Call<ApiEnvelope<DuelDto>> call, @NonNull Throwable t) {
                Log.e(TAG, "accept failed", t);
                if (callback != null) callback.onError(msg(t));
            }
        });
    }

    /** POST /duels/{id}/decline — opponent declines; wager untouched (plan §7.5). */
    public void decline(final long id, final Callback<DuelDto> callback) {
        api.declineDuel(id).enqueue(new retrofit2.Callback<ApiEnvelope<DuelDto>>() {
            @Override
            public void onResponse(@NonNull Call<ApiEnvelope<DuelDto>> call,
                                   @NonNull Response<ApiEnvelope<DuelDto>> response) {
                ApiEnvelope<DuelDto> envelope = response.body();
                if (response.isSuccessful() && envelope != null && envelope.isSuccess()) {
                    if (callback != null) callback.onSuccess(envelope.getData());
                } else if (response.code() == 401) {
                    if (callback != null) callback.onAuthError();
                } else {
                    if (callback != null) callback.onError(extractError(response, envelope));
                }
            }

            @Override
            public void onFailure(@NonNull Call<ApiEnvelope<DuelDto>> call, @NonNull Throwable t) {
                Log.e(TAG, "decline failed", t);
                if (callback != null) callback.onError(msg(t));
            }
        });
    }

    /** POST /duels/{id}/solve — submit an answer (one attempt per player, plan §7.6). */
    public void solve(final long id, final String answer, final Callback<DuelSolveResponseDto> callback) {
        Map<String, Object> body = new HashMap<>();
        body.put("answer", answer);

        api.solveDuel(id, body).enqueue(new retrofit2.Callback<ApiEnvelope<DuelSolveResponseDto>>() {
            @Override
            public void onResponse(@NonNull Call<ApiEnvelope<DuelSolveResponseDto>> call,
                                   @NonNull Response<ApiEnvelope<DuelSolveResponseDto>> response) {
                ApiEnvelope<DuelSolveResponseDto> envelope = response.body();
                if (response.isSuccessful() && envelope != null && envelope.isSuccess()) {
                    if (callback != null) callback.onSuccess(envelope.getData());
                } else if (response.code() == 401) {
                    if (callback != null) callback.onAuthError();
                } else {
                    if (callback != null) callback.onError(extractError(response, envelope));
                }
            }

            @Override
            public void onFailure(@NonNull Call<ApiEnvelope<DuelSolveResponseDto>> call, @NonNull Throwable t) {
                Log.e(TAG, "solve failed", t);
                if (callback != null) callback.onError(msg(t));
            }
        });
    }

    private String msg(Throwable t) {
        return t.getMessage() == null ? "Network error" : t.getMessage();
    }

    private String extractError(Response<?> response, @Nullable ApiEnvelope<?> envelope) {
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
