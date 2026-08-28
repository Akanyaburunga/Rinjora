package rw.martinhardware.mymartin.data;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.gson.Gson;

import java.io.IOException;

import io.objectbox.Box;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Response;
import rw.martinhardware.mymartin.MyApp;
import rw.martinhardware.mymartin.entities.RinjoraRiddleSnapshot;
import rw.martinhardware.mymartin.entities.RinjoraRiddleSnapshot_;
import rw.martinhardware.mymartin.network.ApiEnvelope;
import rw.martinhardware.mymartin.network.RinjoraApi;
import rw.martinhardware.mymartin.network.RinjoraApiClient;
import rw.martinhardware.mymartin.network.dto.AnswerResponseDto;
import rw.martinhardware.mymartin.network.dto.HintDto;
import rw.martinhardware.mymartin.network.dto.RevealDto;
import rw.martinhardware.mymartin.network.dto.RiddleDto;
import rw.martinhardware.mymartin.network.dto.ShareDto;

/**
 * Offline-first repository for the Rinjora play loop (plan §2.2, §2.8–§2.9):
 * fetch/cache a single riddle, request hints, submit an answer, and reveal
 * (learning mode). The confidential {@code answer} is <b>never</b> cached to
 * ObjectBox — it is only held transiently in memory for the active screen.
 */
public class RinjoraRiddleRepository {

    private static final String TAG = "RinjoraRiddleRepository";
    private final Gson gson = new Gson();

    /** Result holder for actions (hint/answer/reveal) and riddle fetches. */
    public interface Callback<T> {
        void onSuccess(T result);

        void onAuthError();

        void onError(String message);
    }

    /** Cached riddle / loaded riddle bundle passed to the play UI. */
    public static class RiddleBundle {
        public final RinjoraRiddleSnapshot snapshot;
        public final String hint1;
        public final String hint2;

        public RiddleBundle(RinjoraRiddleSnapshot snapshot, String hint1, String hint2) {
            this.snapshot = snapshot;
            this.hint1 = hint1;
            this.hint2 = hint2;
        }
    }

    private final Context context;
    private final RinjoraApi api;

    public RinjoraRiddleRepository(Context context) {
        this.context = context.getApplicationContext();
        this.api = RinjoraApiClient.get(context).api();
    }

    /** GET /riddles/{id} and cache the result so the screen survives rotation/offline. */
    public void fetchRiddle(final long riddleId, final Callback<RiddleBundle> callback) {
        api.riddle(riddleId).enqueue(new retrofit2.Callback<ApiEnvelope<RiddleDto>>() {
            @Override
            public void onResponse(@NonNull Call<ApiEnvelope<RiddleDto>> call,
                                   @NonNull Response<ApiEnvelope<RiddleDto>> response) {
                ApiEnvelope<RiddleDto> envelope = response.body();
                if (response.isSuccessful() && envelope != null && envelope.isSuccess()
                        && envelope.getData() != null) {
                    RiddleDto dto = envelope.getData();
                    RinjoraRiddleSnapshot snapshot = toEntity(dto);
                    snapshot.setFetchedAt(System.currentTimeMillis());
                    save(snapshot);
                    if (callback != null) {
                        callback.onSuccess(new RiddleBundle(snapshot, dto.getHint(), dto.getHint2()));
                    }
                } else if (response.code() == 401) {
                    if (callback != null) callback.onAuthError();
                } else {
                    if (callback != null) callback.onError(extractError(response, envelope));
                }
            }

            @Override
            public void onFailure(@NonNull Call<ApiEnvelope<RiddleDto>> call, @NonNull Throwable t) {
                Log.e(TAG, "fetchRiddle failed", t);
                if (callback != null) callback.onError(msg(t));
            }
        });
    }

    /** Cached snapshot for a riddle id, or null. */
    public RinjoraRiddleSnapshot getCached(long riddleId) {
        try {
            Box<RinjoraRiddleSnapshot> box =
                    ((MyApp) context.getApplicationContext()).getBoxStore().boxFor(RinjoraRiddleSnapshot.class);
            return box.query().equal(RinjoraRiddleSnapshot_.riddleId, riddleId)
                    .orderDesc(RinjoraRiddleSnapshot_.fetchedAt).build().findFirst();
        } catch (Exception e) {
            return null;
        }
    }

    /** GET /riddles/{id}/hint — returns the revealed hints + new hintsRevealed count. */
    public void requestHint(final long riddleId, final Callback<HintDto> callback) {
        api.hint(riddleId).enqueue(new retrofit2.Callback<ApiEnvelope<HintDto>>() {
            @Override
            public void onResponse(@NonNull Call<ApiEnvelope<HintDto>> call,
                                   @NonNull Response<ApiEnvelope<HintDto>> response) {
                ApiEnvelope<HintDto> envelope = response.body();
                if (response.isSuccessful() && envelope != null && envelope.isSuccess()
                        && envelope.getData() != null) {
                    HintDto hint = envelope.getData();
                    RinjoraRiddleSnapshot cached = getCached(riddleId);
                    if (cached != null) {
                        cached.setHintsRevealed(hint.getHintsRevealed());
                        save(cached);
                    }
                    if (callback != null) callback.onSuccess(hint);
                } else if (response.code() == 401) {
                    if (callback != null) callback.onAuthError();
                } else {
                    if (callback != null) callback.onError(extractError(response, envelope));
                }
            }

            @Override
            public void onFailure(@NonNull Call<ApiEnvelope<HintDto>> call, @NonNull Throwable t) {
                Log.e(TAG, "requestHint failed", t);
                if (callback != null) callback.onError(msg(t));
            }
        });
    }

    /** POST /riddles/{id}/answer — returns the grading result. */
    public void submitAnswer(final long riddleId, String answer, final String hint1,
                             final String hint2, final Callback<AnswerResponseDto> callback) {
        java.util.Map<String, Object> body = new java.util.HashMap<>();
        body.put("answer", answer);
        api.answer(riddleId, body).enqueue(new retrofit2.Callback<ApiEnvelope<AnswerResponseDto>>() {
            @Override
            public void onResponse(@NonNull Call<ApiEnvelope<AnswerResponseDto>> call,
                                   @NonNull Response<ApiEnvelope<AnswerResponseDto>> response) {
                ApiEnvelope<AnswerResponseDto> envelope = response.body();
                if (response.isSuccessful() && envelope != null && envelope.isSuccess()
                        && envelope.getData() != null) {
                    AnswerResponseDto result = envelope.getData();
                    maybeMarkSolved(riddleId, result);
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

    /**
     * POST /riddles/{id}/reveal (learning mode). Returns the answer with <b>no</b>
     * reward. The answer is returned by callback but never cached.
     */
    public void reveal(final long riddleId, final Callback<RevealDto> callback) {
        api.reveal(riddleId).enqueue(new retrofit2.Callback<ApiEnvelope<RevealDto>>() {
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

    /** POST/DELETE /me/favorites/{riddle} (plan §6.1). Favorite or unfavorite a riddle. */
    public void setFavorite(final long riddleId, final boolean favorite, final Callback<Void> callback) {
        retrofit2.Call<ApiEnvelope<Void>> call =
                favorite ? api.addFavorite(riddleId) : api.removeFavorite(riddleId);
        call.enqueue(new retrofit2.Callback<ApiEnvelope<Void>>() {
            @Override
            public void onResponse(@NonNull retrofit2.Call<ApiEnvelope<Void>> call,
                                   @NonNull retrofit2.Response<ApiEnvelope<Void>> response) {
                ApiEnvelope<Void> envelope = response.body();
                if (response.isSuccessful() && envelope != null && envelope.isSuccess()) {
                    if (callback != null) callback.onSuccess(null);
                } else if (response.code() == 401) {
                    if (callback != null) callback.onAuthError();
                } else {
                    if (callback != null) callback.onError(extractError(response, envelope));
                }
            }

            @Override
            public void onFailure(@NonNull retrofit2.Call<ApiEnvelope<Void>> call, @NonNull Throwable t) {
                Log.e(TAG, "setFavorite failed", t);
                if (callback != null) callback.onError(msg(t));
            }
        });
    }

    /** POST /riddles/{id}/share (plan §6.2) — returns a short share URL/code. */
    public void shareRiddle(final long riddleId, final Callback<ShareDto> callback) {
        api.share(riddleId, new java.util.HashMap<String, Object>()).enqueue(
                new retrofit2.Callback<ApiEnvelope<ShareDto>>() {
                    @Override
                    public void onResponse(@NonNull retrofit2.Call<ApiEnvelope<ShareDto>> call,
                                           @NonNull retrofit2.Response<ApiEnvelope<ShareDto>> response) {
                        ApiEnvelope<ShareDto> envelope = response.body();
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
                    public void onFailure(@NonNull retrofit2.Call<ApiEnvelope<ShareDto>> call,
                                          @NonNull Throwable t) {
                        Log.e(TAG, "share failed", t);
                        if (callback != null) callback.onError(msg(t));
                    }
                });
    }

    private void maybeMarkSolved(long riddleId, AnswerResponseDto result) {
        if (result.isCorrect()) {
            RinjoraRiddleSnapshot cached = getCached(riddleId);
            if (cached != null && !cached.isSolved()) {
                cached.setSolved(true);
                save(cached);
            }
        }
    }

    private void save(RinjoraRiddleSnapshot snapshot) {
        Box<RinjoraRiddleSnapshot> box =
                ((MyApp) context.getApplicationContext()).getBoxStore().boxFor(RinjoraRiddleSnapshot.class);
        box.put(snapshot);
    }

    private RinjoraRiddleSnapshot toEntity(RiddleDto dto) {
        RinjoraRiddleSnapshot s = new RinjoraRiddleSnapshot();
        s.setRiddleId(dto.getId());
        if (dto.getCategory() != null) {
            s.setCategoryId(dto.getCategory().getId());
            s.setCategoryName(dto.getCategory().getName());
        }
        s.setQuestion(dto.getQuestion());
        s.setDifficulty(dto.getDifficulty());
        s.setRiddleType(dto.getRiddleType());
        s.setHintsRevealed(dto.getHintsRevealed());
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
