package org.kazinduzi.rinjora.data;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.gson.Gson;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Response;
import org.kazinduzi.rinjora.network.ApiEnvelope;
import org.kazinduzi.rinjora.network.RinjoraApi;
import org.kazinduzi.rinjora.network.RinjoraApiClient;
import org.kazinduzi.rinjora.network.dto.RoundAnswerDto;
import org.kazinduzi.rinjora.network.dto.RoundCompleteDto;
import org.kazinduzi.rinjora.network.dto.RoundItemDto;
import org.kazinduzi.rinjora.network.dto.RoundStartDto;

/**
 * Thin, strictly-online repository for the round-of-N game loop (parity plan §5).
 * Round state is server-owned: the client only previews {@code item.position} while
 * server-side {@code index} is authoritative. Nothing is persisted to ObjectBox —
 * solved state is never cached.
 */
public class RinjoraRoundRepository {

    private static final String TAG = "RinjoraRoundRepository";
    private final Gson gson = new Gson();

    /** Result holder mirroring the other repositories. */
    public interface Callback<T> {
        void onSuccess(T result);

        void onAuthError();

        void onError(String message);
    }

    private final Context context;
    private final RinjoraApi api;

    public RinjoraRoundRepository(Context context) {
        this.context = context.getApplicationContext();
        this.api = RinjoraApiClient.get(context).api();
    }

    /** POST /games/{mode}/rounds — a fresh round, first item (never contains the answer). */
    public void start(String mode, Integer level, final Callback<RoundStartDto> callback) {
        Map<String, Object> body = new HashMap<>();
        if (level != null) body.put("level", level);
        api.startRound(mode, body).enqueue(new retrofit2.Callback<ApiEnvelope<RoundStartDto>>() {
            @Override
            public void onResponse(@NonNull Call<ApiEnvelope<RoundStartDto>> call,
                                   @NonNull Response<ApiEnvelope<RoundStartDto>> response) {
                ApiEnvelope<RoundStartDto> envelope = response.body();
                if (response.isSuccessful() && envelope != null && envelope.isSuccess()
                        && envelope.getData() != null) {
                    dispatch(callback, envelope.getData());
                } else {
                    handleEnvelopeFailure(response, envelope, callback);
                }
            }

            @Override
            public void onFailure(@NonNull Call<ApiEnvelope<RoundStartDto>> call, @NonNull Throwable t) {
                Log.e(TAG, "start failed", t);
                fail(callback, msg(t));
            }
        });
    }

    /** GET /games/{mode}/rounds/{round} — server-side resume; restores score/streak state. */
    public void resume(String mode, long roundId, final Callback<RoundStartDto> callback) {
        api.resumeRound(mode, roundId).enqueue(new retrofit2.Callback<ApiEnvelope<RoundStartDto>>() {
            @Override
            public void onResponse(@NonNull Call<ApiEnvelope<RoundStartDto>> call,
                                   @NonNull Response<ApiEnvelope<RoundStartDto>> response) {
                ApiEnvelope<RoundStartDto> envelope = response.body();
                if (response.isSuccessful() && envelope != null && envelope.isSuccess()
                        && envelope.getData() != null) {
                    // Plan G-1: per-position answered state is server-side; positions answered
                    // server-side come back with answered/revealed_answer filled in.
                    dispatch(callback, envelope.getData());
                } else {
                    handleEnvelopeFailure(response, envelope, callback);
                }
            }

            @Override
            public void onFailure(@NonNull Call<ApiEnvelope<RoundStartDto>> call, @NonNull Throwable t) {
                Log.e(TAG, "resume failed", t);
                fail(callback, msg(t));
            }
        });
    }

    /** GET /games/{mode}/rounds/{round}/items/{position} — view a position (back-nav / resume). */
    public void item(String mode, long roundId, int position, final Callback<RoundItemDto> callback) {
        api.item(mode, roundId, position).enqueue(new retrofit2.Callback<ApiEnvelope<RoundItemDto>>() {
            @Override
            public void onResponse(@NonNull Call<ApiEnvelope<RoundItemDto>> call,
                                   @NonNull Response<ApiEnvelope<RoundItemDto>> response) {
                ApiEnvelope<RoundItemDto> envelope = response.body();
                if (response.isSuccessful() && envelope != null && envelope.isSuccess()
                        && envelope.getData() != null) {
                    dispatch(callback, envelope.getData());
                } else {
                    handleEnvelopeFailure(response, envelope, callback);
                }
            }

            @Override
            public void onFailure(@NonNull Call<ApiEnvelope<RoundItemDto>> call, @NonNull Throwable t) {
                Log.e(TAG, "item failed", t);
                fail(callback, msg(t));
            }
        });
    }

    /** POST .../items/{position}/answer — typed answer (sokwe/hera). Flat grade back. */
    public void answer(String mode, long roundId, int position, String answerText,
                       final Callback<RoundAnswerDto> callback) {
        Map<String, Object> body = new HashMap<>();
        body.put("answer", answerText);
        answerCall(api.answerItem(mode, roundId, position, body), callback);
    }

    /** POST .../items/{position}/answer — option pick (tuja). Flat grade back. */
    public void answerOption(String mode, long roundId, int position, String option,
                             final Callback<RoundAnswerDto> callback) {
        Map<String, Object> body = new HashMap<>();
        body.put("option", option);
        answerCall(api.answerItem(mode, roundId, position, body), callback);
    }

    /** POST .../items/{position}/skip — concede/skip; claims the reveal. Flat grade back. */
    public void skip(String mode, long roundId, int position, final Callback<RoundAnswerDto> callback) {
        answerCall(api.skipItem(mode, roundId, position), callback);
    }

    /** POST .../complete — finish the round; {@code performance} = top/mid/low. */
    public void complete(String mode, long roundId, final Callback<RoundCompleteDto> callback) {
        api.completeRound(mode, roundId).enqueue(new retrofit2.Callback<ApiEnvelope<RoundCompleteDto>>() {
            @Override
            public void onResponse(@NonNull Call<ApiEnvelope<RoundCompleteDto>> call,
                                   @NonNull Response<ApiEnvelope<RoundCompleteDto>> response) {
                ApiEnvelope<RoundCompleteDto> envelope = response.body();
                if (response.isSuccessful() && envelope != null && envelope.isSuccess()
                        && envelope.getData() != null) {
                    dispatch(callback, envelope.getData());
                } else {
                    handleEnvelopeFailure(response, envelope, callback);
                }
            }

            @Override
            public void onFailure(@NonNull Call<ApiEnvelope<RoundCompleteDto>> call, @NonNull Throwable t) {
                Log.e(TAG, "complete failed", t);
                fail(callback, msg(t));
            }
        });
    }

    /**
     * Shared handling for the two flat-answer endpoints. 2xx with a body is a real game
     * outcome (correct, wrong, conceded, or skip-claimed); 401 is an auth error.
     */
    private void answerCall(final Call<RoundAnswerDto> call, final Callback<RoundAnswerDto> callback) {
        call.enqueue(new retrofit2.Callback<RoundAnswerDto>() {
            @Override
            public void onResponse(@NonNull Call<RoundAnswerDto> c,
                                   @NonNull Response<RoundAnswerDto> response) {
                RoundAnswerDto result = response.body();
                if (response.isSuccessful() && result != null) {
                    dispatch(callback, result);
                } else if (response.code() == 401) {
                    if (callback != null) callback.onAuthError();
                } else {
                    fail(callback, extractError(response));
                }
            }

            @Override
            public void onFailure(@NonNull Call<RoundAnswerDto> c, @NonNull Throwable t) {
                Log.e(TAG, "answerCall failed", t);
                fail(callback, msg(t));
            }
        });
    }

    private <T> void handleEnvelopeFailure(Response<ApiEnvelope<T>> response,
                                           ApiEnvelope<T> envelope,
                                           Callback<T> callback) {
        if (response.code() == 401) {
            if (callback != null) callback.onAuthError();
            return;
        }
        if (envelope != null && envelope.getMessage() != null && !envelope.getMessage().isEmpty()) {
            fail(callback, envelope.getMessage());
            return;
        }
        fail(callback, extractError(response));
    }

    private <T> void dispatch(Callback<T> callback, T result) {
        if (callback != null) callback.onSuccess(result);
    }

    private <T> void fail(Callback<T> callback, String message) {
        if (callback != null) callback.onError(message);
    }

    private String extractError(Response<?> response) {
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

    private String msg(Throwable t) {
        return t.getMessage() == null ? "Network error" : t.getMessage();
    }
}