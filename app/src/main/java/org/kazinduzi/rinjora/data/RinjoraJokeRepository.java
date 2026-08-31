package org.kazinduzi.rinjora.data;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import io.objectbox.Box;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Response;
import org.kazinduzi.rinjora.MyApp;
import org.kazinduzi.rinjora.entities.RinjoraJokeSnapshot;
import org.kazinduzi.rinjora.entities.RinjoraJokeSnapshot_;
import org.kazinduzi.rinjora.network.ApiEnvelope;
import org.kazinduzi.rinjora.network.RinjoraApi;
import org.kazinduzi.rinjora.network.RinjoraApiClient;
import org.kazinduzi.rinjora.network.dto.JokeAnswerResponseDto;
import org.kazinduzi.rinjora.network.dto.JokeRoundDto;
import org.kazinduzi.rinjora.network.dto.RevealDto;

/**
 * Offline-first repository for the Tujajure joke mode (parity plan §3): fetch a round
 * (setup + 4 pre-shuffled options), submit a chosen option, fetch the next round, and
 * reveal (learning). The correct punchline is <b>never</b> cached to ObjectBox — only
 * the 4 order-preserved options and the setup are persisted.
 */
public class RinjoraJokeRepository {

    private static final String TAG = "RinjoraJokeRepository";
    private final Gson gson = new Gson();

    /** Result holder mirroring the other repositories. */
    public interface Callback<T> {
        void onSuccess(T result);

        void onAuthError();

        void onError(String message);
    }

    /** Cached / loaded joke round bundle passed to the play UI. */
    public static class JokeBundle {
        public final RinjoraJokeSnapshot snapshot;
        public final List<String> options;

        public JokeBundle(RinjoraJokeSnapshot snapshot, List<String> options) {
            this.snapshot = snapshot;
            this.options = options;
        }
    }

    private final Context context;
    private final RinjoraApi api;

    public RinjoraJokeRepository(Context context) {
        this.context = context.getApplicationContext();
        this.api = RinjoraApiClient.get(context).api();
    }

    private static final Type STRING_LIST_TYPE = new TypeToken<List<String>>() {
    }.getType();

    /** GET /jokes/round — the current round (server-order options). */
    public void getRound(final Callback<JokeBundle> callback) {
        api.jokeRound().enqueue(new retrofit2.Callback<ApiEnvelope<JokeRoundDto>>() {
            @Override
            public void onResponse(@NonNull Call<ApiEnvelope<JokeRoundDto>> call,
                                   @NonNull Response<ApiEnvelope<JokeRoundDto>> response) {
                ApiEnvelope<JokeRoundDto> envelope = response.body();
                if (response.isSuccessful() && envelope != null && envelope.isSuccess()
                        && envelope.getData() != null) {
                    JokeRoundDto dto = envelope.getData();
                    RinjoraJokeSnapshot snapshot = toEntity(dto);
                    snapshot.setFetchedAt(System.currentTimeMillis());
                    save(snapshot);
                    if (callback != null) callback.onSuccess(new JokeBundle(snapshot, dto.getOptions()));
                } else if (response.code() == 401) {
                    if (callback != null) callback.onAuthError();
                } else {
                    if (callback != null) callback.onError(extractError(response, envelope));
                }
            }

            @Override
            public void onFailure(@NonNull Call<ApiEnvelope<JokeRoundDto>> call, @NonNull Throwable t) {
                Log.e(TAG, "getRound failed", t);
                if (callback != null) callback.onError(msg(t));
            }
        });
    }

    /** GET /jokes/next — the next unsolved round (404 → nothing left). */
    public void getNext(final Callback<JokeBundle> callback) {
        api.nextJoke().enqueue(new retrofit2.Callback<ApiEnvelope<JokeRoundDto>>() {
            @Override
            public void onResponse(@NonNull Call<ApiEnvelope<JokeRoundDto>> call,
                                   @NonNull Response<ApiEnvelope<JokeRoundDto>> response) {
                ApiEnvelope<JokeRoundDto> envelope = response.body();
                if (response.isSuccessful() && envelope != null && envelope.isSuccess()
                        && envelope.getData() != null) {
                    JokeRoundDto dto = envelope.getData();
                    RinjoraJokeSnapshot snapshot = toEntity(dto);
                    snapshot.setFetchedAt(System.currentTimeMillis());
                    save(snapshot);
                    if (callback != null) callback.onSuccess(new JokeBundle(snapshot, dto.getOptions()));
                } else if (response.code() == 404) {
                    if (callback != null) callback.onError("No more jokes to solve. Well done!");
                } else if (response.code() == 401) {
                    if (callback != null) callback.onAuthError();
                } else {
                    if (callback != null) callback.onError(extractError(response, envelope));
                }
            }

            @Override
            public void onFailure(@NonNull Call<ApiEnvelope<JokeRoundDto>> call, @NonNull Throwable t) {
                Log.e(TAG, "getNext failed", t);
                if (callback != null) callback.onError(msg(t));
            }
        });
    }

    /**
     * POST /jokes/{id}/answer — returns the grade. Both a correct and a wrong attempt
     * are legitimate outcomes and arrive via {@code onSuccess}; distinguish with
     * {@link JokeAnswerResponseDto#isCorrect()}. On a wrong attempt the backend includes
     * the correct punchline in {@code answer} which the UI uses to highlight the right option.
     */
    public void submitAnswer(final long jokeId, String option, final Callback<JokeAnswerResponseDto> callback) {
        java.util.Map<String, Object> body = new java.util.HashMap<>();
        body.put("option", option);
        api.answerJoke(jokeId, body).enqueue(new retrofit2.Callback<ApiEnvelope<JokeAnswerResponseDto>>() {
            @Override
            public void onResponse(@NonNull Call<ApiEnvelope<JokeAnswerResponseDto>> call,
                                   @NonNull Response<ApiEnvelope<JokeAnswerResponseDto>> response) {
                JokeAnswerResponseDto result = envelopeData(response);
                if (result != null) {
                    // Whether correct or wrong, this is a real game outcome to render.
                    if (result.isCorrect()) maybeMarkSolved(jokeId);
                    if (callback != null) callback.onSuccess(result);
                } else if (response.code() == 401) {
                    if (callback != null) callback.onAuthError();
                } else {
                    if (callback != null) callback.onError(extractError(response, response.body()));
                }
            }

            @Override
            public void onFailure(@NonNull Call<ApiEnvelope<JokeAnswerResponseDto>> call, @NonNull Throwable t) {
                Log.e(TAG, "submitAnswer failed", t);
                if (callback != null) callback.onError(msg(t));
            }
        });
    }

    /**
     * POST /jokes/{id}/reveal (learning mode, no reward). Returns the punchline via callback
     * but it is <b>never</b> cached.
     */
    public void reveal(final long jokeId, final Callback<RevealDto> callback) {
        api.revealJoke(jokeId).enqueue(new retrofit2.Callback<ApiEnvelope<RevealDto>>() {
            @Override
            public void onResponse(@NonNull Call<ApiEnvelope<RevealDto>> call,
                                   @NonNull Response<ApiEnvelope<RevealDto>> response) {
                ApiEnvelope<RevealDto> envelope = response.body();
                if (response.isSuccessful() && envelope != null && envelope.isSuccess()
                        && envelope.getData() != null) {
                    if (callback != null) callback.onSuccess(envelope.getData());
                } else if (response.code() == 401) {
                    if (callback != null) callback.onAuthError();
                } else {
                    if (callback != null) callback.onError(extractError(response, envelope));
                }
            }

            @Override
            public void onFailure(@NonNull Call<ApiEnvelope<RevealDto>> call, @NonNull Throwable t) {
                Log.e(TAG, "reveal failed", t);
                if (callback != null) callback.onError(msg(t));
            }
        });
    }

    /** The cached round snapshot (played most recently), or null. */
    public RinjoraJokeSnapshot getCached() {
        try {
            Box<RinjoraJokeSnapshot> box =
                    ((MyApp) context.getApplicationContext()).getBoxStore().boxFor(RinjoraJokeSnapshot.class);
            return box.query().orderDesc(RinjoraJokeSnapshot_.fetchedAt).build().findFirst();
        } catch (Exception e) {
            return null;
        }
    }

    /** Local fallback total of solved jokes cached on device (parity plan §4.1). */
    public int countLocalSolved() {
        try {
            Box<RinjoraJokeSnapshot> box =
                    ((MyApp) context.getApplicationContext()).getBoxStore().boxFor(RinjoraJokeSnapshot.class);
            return (int) box.query().equal(RinjoraJokeSnapshot_.solved, true).build().count();
        } catch (Exception e) {
            return 0;
        }
    }

    /** Deserialise the options list from an entity's JSON (server order). */
    public List<String> optionsOf(RinjoraJokeSnapshot snapshot) {
        if (snapshot == null || snapshot.getOptionsJson() == null || snapshot.getOptionsJson().isEmpty()) {
            return new ArrayList<>();
        }
        try {
            List<String> list = gson.fromJson(snapshot.getOptionsJson(), STRING_LIST_TYPE);
            return list != null ? list : new ArrayList<>();
        } catch (RuntimeException e) {
            return new ArrayList<>();
        }
    }

    /**
     * Extract the {@code data} DTO from a response body even when the envelope's
     * {@code success} flag is false but {@code data} is still present (the joke wrong-answer
     * case, per parity plan §3.2). Returns null when there is no DTO to render.
     */
    private JokeAnswerResponseDto envelopeData(Response<ApiEnvelope<JokeAnswerResponseDto>> response) {
        ApiEnvelope<JokeAnswerResponseDto> envelope = response.body();
        if (envelope != null && envelope.getData() != null) {
            return envelope.getData();
        }
        if (!response.isSuccessful()) {
            // Fallback: some backends return the wrong-answer DTO inside an error body.
            ResponseBody body = response.errorBody();
            if (body != null) {
                try {
                    ApiEnvelope<JokeAnswerResponseDto> parsed = gson.fromJson(
                            body.string(), new TypeToken<ApiEnvelope<JokeAnswerResponseDto>>() {
                            }.getType());
                    if (parsed != null && parsed.getData() != null) {
                        return parsed.getData();
                    }
                } catch (IOException | RuntimeException ignored) {
                    // fall through
                }
            }
        }
        return null;
    }

    private void maybeMarkSolved(long jokeId) {
        RinjoraJokeSnapshot cached = getCached();
        if (cached != null && cached.getJokeId() == jokeId && !cached.isSolved()) {
            cached.setSolved(true);
            save(cached);
        }
    }

    private void save(RinjoraJokeSnapshot snapshot) {
        Box<RinjoraJokeSnapshot> box =
                ((MyApp) context.getApplicationContext()).getBoxStore().boxFor(RinjoraJokeSnapshot.class);
        box.put(snapshot);
    }

    private RinjoraJokeSnapshot toEntity(JokeRoundDto dto) {
        RinjoraJokeSnapshot s = new RinjoraJokeSnapshot();
        s.setJokeId(dto.getJokeId());
        s.setSetup(dto.getSetup());
        s.setOptionsJson(gson.toJson(dto.getOptions(), STRING_LIST_TYPE));
        s.setSolved(false);
        s.setRawJson(gson.toJson(dto));
        return s;
    }

    private String msg(Throwable t) {
        return t.getMessage() == null ? "Network error" : t.getMessage();
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
