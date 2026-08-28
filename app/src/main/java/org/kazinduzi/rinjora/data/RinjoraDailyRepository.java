package org.kazinduzi.rinjora.data;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import java.io.IOException;

import io.objectbox.Box;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Response;
import org.kazinduzi.rinjora.MyApp;
import org.kazinduzi.rinjora.entities.RinjoraDailySnapshot;
import org.kazinduzi.rinjora.entities.RinjoraDailySnapshot_;
import org.kazinduzi.rinjora.network.ApiEnvelope;
import org.kazinduzi.rinjora.network.RinjoraApi;
import org.kazinduzi.rinjora.network.RinjoraApiClient;
import org.kazinduzi.rinjora.network.dto.DailyRiddleDto;
import org.kazinduzi.rinjora.network.dto.DailyStatusDto;
import org.kazinduzi.rinjora.network.dto.FreezeResponseDto;

/**
 * Offline-first repository for the Daily riddle screen (plan §2.4–§2.6, §2.12).
 * Merges {@code GET /riddles/daily} and {@code GET /riddles/daily/status} into a
 * single {@link RinjoraDailySnapshot}; also spends a streak freeze via
 * {@code POST /riddles/streak/freeze}.
 */
public class RinjoraDailyRepository {

    private static final String TAG = "RinjoraDailyRepository";

    public interface Callback<T> {
        void onSuccess(T result);

        void onAuthError();

        void onError(String message);
    }

    private final Context context;
    private final RinjoraApi api;

    public RinjoraDailyRepository(Context context) {
        this.context = context.getApplicationContext();
        this.api = RinjoraApiClient.get(context).api();
    }

    /** Fetch daily status + daily riddle and cache into a snapshot. */
    public void load(final Callback<RinjoraDailySnapshot> callback) {
        // Refresh status first, then enrich with the daily riddle details.
        api.dailyStatus().enqueue(new retrofit2.Callback<ApiEnvelope<DailyStatusDto>>() {
            @Override
            public void onResponse(@NonNull Call<ApiEnvelope<DailyStatusDto>> call,
                                   @NonNull Response<ApiEnvelope<DailyStatusDto>> response) {
                ApiEnvelope<DailyStatusDto> envelope = response.body();
                if (response.isSuccessful() && envelope != null && envelope.isSuccess()
                        && envelope.getData() != null) {
                    fetchDaily(new Runnable() {
                        @Override
                        public void run() {
                            if (callback != null) callback.onSuccess(getCached());
                        }
                    }, envelope.getData());
                } else if (response.code() == 401) {
                    if (callback != null) callback.onAuthError();
                } else {
                    if (callback != null) callback.onError(extractError(response, envelope));
                }
            }

            @Override
            public void onFailure(@NonNull Call<ApiEnvelope<DailyStatusDto>> call, @NonNull Throwable t) {
                Log.e(TAG, "load status failed", t);
                if (callback != null) callback.onError(msg(t));
            }
        });
    }

    private void fetchDaily(final Runnable onDone, DailyStatusDto status) {
        api.dailyRiddle().enqueue(new retrofit2.Callback<ApiEnvelope<DailyRiddleDto>>() {
            @Override
            public void onResponse(@NonNull Call<ApiEnvelope<DailyRiddleDto>> call,
                                   @NonNull Response<ApiEnvelope<DailyRiddleDto>> response) {
                ApiEnvelope<DailyRiddleDto> envelope = response.body();
                DailyRiddleDto daily = (response.isSuccessful() && envelope != null
                        && envelope.isSuccess()) ? envelope.getData() : null;
                save(toEntity(status, daily));
                onDone.run();
            }

            @Override
            public void onFailure(@NonNull Call<ApiEnvelope<DailyRiddleDto>> call, @NonNull Throwable t) {
                Log.e(TAG, "load daily failed", t);
                save(toEntity(status, null));
                onDone.run();
            }
        });
    }

    /** Latest cached snapshot, or null. */
    public RinjoraDailySnapshot getCached() {
        try {
            Box<RinjoraDailySnapshot> box =
                    ((MyApp) context.getApplicationContext()).getBoxStore().boxFor(RinjoraDailySnapshot.class);
            return box.query().orderDesc(RinjoraDailySnapshot_.fetchedAt).build().findFirst();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Latest cached snapshot <b>only if it was fetched on the current local
     * calendar date</b>, else null (plan §11). Guards against rendering a stale
     * previous-day's daily riddle as "today" when the cache crosses midnight —
     * the daily riddle is deterministic per user/date, so the server is always
     * fresh-authoritative.
     */
    public RinjoraDailySnapshot getCachedForToday() {
        RinjoraDailySnapshot snap = getCached();
        if (snap == null) return null;
        java.util.Calendar now = java.util.Calendar.getInstance();
        java.util.Calendar fetched = java.util.Calendar.getInstance();
        fetched.setTimeInMillis(snap.getFetchedAt());
        return sameLocalDate(now, fetched) ? snap : null;
    }

    private static boolean sameLocalDate(java.util.Calendar a, java.util.Calendar b) {
        return a.get(java.util.Calendar.YEAR) == b.get(java.util.Calendar.YEAR)
                && a.get(java.util.Calendar.DAY_OF_YEAR) == b.get(java.util.Calendar.DAY_OF_YEAR);
    }

    /** Spend one freeze to protect today's streak (plan §2.12). */
    public void freeze(final Callback<FreezeResponseDto> callback) {
        api.freezeStreak().enqueue(new retrofit2.Callback<ApiEnvelope<FreezeResponseDto>>() {
            @Override
            public void onResponse(@NonNull Call<ApiEnvelope<FreezeResponseDto>> call,
                                   @NonNull Response<ApiEnvelope<FreezeResponseDto>> response) {
                ApiEnvelope<FreezeResponseDto> envelope = response.body();
                if (response.isSuccessful() && envelope != null && envelope.isSuccess()
                        && envelope.getData() != null) {
                    RinjoraDailySnapshot cached = getCached();
                    if (cached != null) {
                        cached.setStreakAtRisk(false);
                        if (envelope.getData().getStreak() != null) {
                            cached.setCurrentStreak(envelope.getData().getStreak().getCurrent());
                            cached.setLongestStreak(envelope.getData().getStreak().getLongest());
                        }
                        saveInternal(cached);
                    }
                    if (callback != null) callback.onSuccess(envelope.getData());
                } else if (response.code() == 401) {
                    if (callback != null) callback.onAuthError();
                } else {
                    // 422 means no freezes remain / already frozen today — surface the message.
                    if (callback != null) callback.onError(extractError(response, envelope));
                }
            }

            @Override
            public void onFailure(@NonNull Call<ApiEnvelope<FreezeResponseDto>> call, @NonNull Throwable t) {
                Log.e(TAG, "freeze failed", t);
                if (callback != null) callback.onError(msg(t));
            }
        });
    }

    /** Flatten status + daily into a snapshot. */
    private RinjoraDailySnapshot toEntity(DailyStatusDto status, DailyRiddleDto daily) {
        RinjoraDailySnapshot s = new RinjoraDailySnapshot();
        s.setFetchedAt(System.currentTimeMillis());
        if (status != null) {
            s.setDailyAvailable(status.isDailyAvailable());
            s.setStreakAtRisk(status.isStreakAtRisk());
            s.setPendingChallenges(status.getPendingChallenges());
            if (status.getStreak() != null) {
                s.setCurrentStreak(status.getStreak().getCurrent());
                s.setLongestStreak(status.getStreak().getLongest());
            }
        }
        if (daily != null && daily.getDaily() != null) {
            s.setDailyRiddleId(daily.getDaily().getId());
            s.setDailyQuestion(daily.getDaily().getQuestion());
            s.setDailySolved(daily.getDaily().isSolved());
            if (s.getCurrentStreak() == 0 && daily.getStreak() != null) {
                s.setCurrentStreak(daily.getStreak().getCurrent());
                s.setLongestStreak(daily.getStreak().getLongest());
            }
        }
        return s;
    }

    private void save(RinjoraDailySnapshot snapshot) {
        saveInternal(snapshot);
    }

    private void saveInternal(RinjoraDailySnapshot snapshot) {
        Box<RinjoraDailySnapshot> box =
                ((MyApp) context.getApplicationContext()).getBoxStore().boxFor(RinjoraDailySnapshot.class);
        box.put(snapshot);
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
