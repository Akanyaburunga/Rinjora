package org.kazinduzi.rinjora.data;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.gson.Gson;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import io.objectbox.Box;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Response;
import org.kazinduzi.rinjora.MyApp;
import org.kazinduzi.rinjora.entities.RinjoraProverbSnapshot;
import org.kazinduzi.rinjora.entities.RinjoraProverbSnapshot_;
import org.kazinduzi.rinjora.network.ApiEnvelope;
import org.kazinduzi.rinjora.network.RinjoraApi;
import org.kazinduzi.rinjora.network.RinjoraApiClient;
import org.kazinduzi.rinjora.network.dto.AnswerResponseDto;
import org.kazinduzi.rinjora.network.dto.ProverbDto;
import org.kazinduzi.rinjora.network.dto.RevealDto;

/**
 * Offline-first repository for the Heraheza proverb mode (parity plan §2):
 * fetch/cache proverbs (list + single), submit an answer, and reveal (learning).
 * The confidential {@code answer} is <b>never</b> cached to ObjectBox.
 */
public class RinjoraProverbRepository {

    private static final String TAG = "RinjoraProverbRepository";
    private final Gson gson = new Gson();

    /** Result holder mirroring {@link RinjoraRiddleRepository.Callback}. */
    public interface Callback<T> {
        void onSuccess(T result);

        void onAuthError();

        void onError(String message);
    }

    /** Cached proverb / loaded proverb bundle passed to the play UI. */
    public static class ProverbBundle {
        public final RinjoraProverbSnapshot snapshot;

        public ProverbBundle(RinjoraProverbSnapshot snapshot) {
            this.snapshot = snapshot;
        }
    }

    private final Context context;
    private final RinjoraApi api;

    public RinjoraProverbRepository(Context context) {
        this.context = context.getApplicationContext();
        this.api = RinjoraApiClient.get(context).api();
    }

    /** GET /proverbs — returns the raw DTO list (used by the home/list screen). */
    public void fetchProverbs(final Callback<List<ProverbDto>> callback) {
        api.allProverbs().enqueue(new retrofit2.Callback<ApiEnvelope<List<ProverbDto>>>() {
            @Override
            public void onResponse(@NonNull Call<ApiEnvelope<List<ProverbDto>>> call,
                                   @NonNull Response<ApiEnvelope<List<ProverbDto>>> response) {
                ApiEnvelope<List<ProverbDto>> envelope = response.body();
                if (response.isSuccessful() && envelope != null && envelope.isSuccess()) {
                    List<ProverbDto> list = envelope.getData();
                    if (callback != null) callback.onSuccess(list != null ? list : new ArrayList<ProverbDto>());
                } else if (response.code() == 401) {
                    if (callback != null) callback.onAuthError();
                } else {
                    if (callback != null) callback.onError(extractError(response, envelope));
                }
            }

            @Override
            public void onFailure(@NonNull Call<ApiEnvelope<List<ProverbDto>>> call, @NonNull Throwable t) {
                Log.e(TAG, "fetchProverbs failed", t);
                if (callback != null) callback.onError(msg(t));
            }
        });
    }

    /** GET /proverbs/next — the next unsolved proverb to play (404 → none left). */
    public void fetchNext(final Callback<ProverbBundle> callback) {
        api.nextProverb(null).enqueue(new retrofit2.Callback<ApiEnvelope<ProverbDto>>() {
            @Override
            public void onResponse(@NonNull Call<ApiEnvelope<ProverbDto>> call,
                                   @NonNull Response<ApiEnvelope<ProverbDto>> response) {
                ApiEnvelope<ProverbDto> envelope = response.body();
                if (response.isSuccessful() && envelope != null && envelope.isSuccess()
                        && envelope.getData() != null) {
                    ProverbDto dto = envelope.getData();
                    RinjoraProverbSnapshot snapshot = toEntity(dto);
                    snapshot.setFetchedAt(System.currentTimeMillis());
                    save(snapshot);
                    if (callback != null) callback.onSuccess(new ProverbBundle(snapshot));
                } else if (response.code() == 404) {
                    if (callback != null) callback.onError("No more proverbs to solve. Well done!");
                } else if (response.code() == 401) {
                    if (callback != null) callback.onAuthError();
                } else {
                    if (callback != null) callback.onError(extractError(response, envelope));
                }
            }

            @Override
            public void onFailure(@NonNull Call<ApiEnvelope<ProverbDto>> call, @NonNull Throwable t) {
                Log.e(TAG, "fetchNext failed", t);
                if (callback != null) callback.onError(msg(t));
            }
        });
    }

    /** GET /proverbs/{id} and cache the result so the screen survives rotation/offline. */
    public void fetchProverb(final long proverbId, final Callback<ProverbBundle> callback) {
        api.proverb(proverbId).enqueue(new retrofit2.Callback<ApiEnvelope<ProverbDto>>() {
            @Override
            public void onResponse(@NonNull Call<ApiEnvelope<ProverbDto>> call,
                                   @NonNull Response<ApiEnvelope<ProverbDto>> response) {
                ApiEnvelope<ProverbDto> envelope = response.body();
                if (response.isSuccessful() && envelope != null && envelope.isSuccess()
                        && envelope.getData() != null) {
                    RinjoraProverbSnapshot snapshot = toEntity(envelope.getData());
                    snapshot.setFetchedAt(System.currentTimeMillis());
                    save(snapshot);
                    if (callback != null) callback.onSuccess(new ProverbBundle(snapshot));
                } else if (response.code() == 401) {
                    if (callback != null) callback.onAuthError();
                } else {
                    if (callback != null) callback.onError(extractError(response, envelope));
                }
            }

            @Override
            public void onFailure(@NonNull Call<ApiEnvelope<ProverbDto>> call, @NonNull Throwable t) {
                Log.e(TAG, "fetchProverb failed", t);
                if (callback != null) callback.onError(msg(t));
            }
        });
    }

    /** Cached snapshot for a proverb id, or null. */
    public RinjoraProverbSnapshot getCached(long proverbId) {
        try {
            Box<RinjoraProverbSnapshot> box =
                    ((MyApp) context.getApplicationContext()).getBoxStore().boxFor(RinjoraProverbSnapshot.class);
            return box.query().equal(RinjoraProverbSnapshot_.proverbId, proverbId)
                    .orderDesc(RinjoraProverbSnapshot_.fetchedAt).build().findFirst();
        } catch (Exception e) {
            return null;
        }
    }

    /** Local fallback total of solved proverbs cached on device (parity plan §4.1). */
    public int countLocalSolved() {
        try {
            Box<RinjoraProverbSnapshot> box =
                    ((MyApp) context.getApplicationContext()).getBoxStore().boxFor(RinjoraProverbSnapshot.class);
            return (int) box.query().equal(RinjoraProverbSnapshot_.solved, true).build().count();
        } catch (Exception e) {
            return 0;
        }
    }

    /** POST /proverbs/{id}/answer — returns the grading result (lenient UX). */
    public void submitAnswer(final long proverbId, String answer, final Callback<AnswerResponseDto> callback) {
        java.util.Map<String, Object> body = new java.util.HashMap<>();
        body.put("answer", answer);
        api.answerProverb(proverbId, body).enqueue(new retrofit2.Callback<ApiEnvelope<AnswerResponseDto>>() {
            @Override
            public void onResponse(@NonNull Call<ApiEnvelope<AnswerResponseDto>> call,
                                   @NonNull Response<ApiEnvelope<AnswerResponseDto>> response) {
                ApiEnvelope<AnswerResponseDto> envelope = response.body();
                if (response.isSuccessful() && envelope != null && envelope.isSuccess()
                        && envelope.getData() != null) {
                    AnswerResponseDto result = envelope.getData();
                    if (result.isCorrect()) maybeMarkSolved(proverbId);
                    if (callback != null) callback.onSuccess(result);
                } else if (response.code() == 401) {
                    if (callback != null) callback.onAuthError();
                } else {
                    if (callback != null) callback.onError(extractError(response, envelope));
                }
            }

            @Override
            public void onFailure(@NonNull Call<ApiEnvelope<AnswerResponseDto>> call, @NonNull Throwable t) {
                Log.e(TAG, "submitAnswer failed", t);
                if (callback != null) callback.onError(msg(t));
            }
        });
    }

    /** POST /proverbs/{id}/reveal (learning mode) — returns the answer with <b>no</b> reward. */
    public void reveal(final long proverbId, final Callback<RevealDto> callback) {
        api.revealProverb(proverbId).enqueue(new retrofit2.Callback<ApiEnvelope<RevealDto>>() {
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

    private void maybeMarkSolved(long proverbId) {
        RinjoraProverbSnapshot cached = getCached(proverbId);
        if (cached != null && !cached.isSolved()) {
            cached.setSolved(true);
            save(cached);
        }
    }

    private void save(RinjoraProverbSnapshot snapshot) {
        Box<RinjoraProverbSnapshot> box =
                ((MyApp) context.getApplicationContext()).getBoxStore().boxFor(RinjoraProverbSnapshot.class);
        box.put(snapshot);
    }

    private RinjoraProverbSnapshot toEntity(ProverbDto dto) {
        RinjoraProverbSnapshot s = new RinjoraProverbSnapshot();
        s.setProverbId(dto.getId());
        if (dto.getCategory() != null) {
            s.setCategoryId(dto.getCategory().getId());
            s.setCategoryName(dto.getCategory().getName());
        }
        s.setQuestion(dto.getQuestion());
        s.setDifficulty(dto.getDifficulty());
        s.setSource(dto.getSource());
        s.setSolved(dto.isSolved());
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
