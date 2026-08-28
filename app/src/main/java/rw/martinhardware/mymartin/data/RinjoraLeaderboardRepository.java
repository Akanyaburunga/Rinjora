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
import rw.martinhardware.mymartin.entities.RinjoraLeaderboardSnapshot;
import rw.martinhardware.mymartin.entities.RinjoraLeaderboardSnapshot_;
import rw.martinhardware.mymartin.network.RinjoraApi;
import rw.martinhardware.mymartin.network.RinjoraApiClient;
import rw.martinhardware.mymartin.network.dto.LeaderboardEnvelope;

/**
 * Offline-first repository for the leaderboard (plan §5.1). Fetches a page for a
 * given period filter and caches the last envelope so the board renders offline;
 * also re-parses the cached JSON into a {@link LeaderboardEnvelope}.
 */
public class RinjoraLeaderboardRepository {

    private static final String TAG = "RinjoraLeaderboardRepo";
    private final Gson gson = new Gson();

    public interface Callback {
        void onSuccess(LeaderboardEnvelope envelope);

        void onAuthError();

        void onError(String message);
    }

    private final Context context;
    private final RinjoraApi api;

    public RinjoraLeaderboardRepository(Context context) {
        this.context = context.getApplicationContext();
        this.api = RinjoraApiClient.get(context).api();
    }

    /** GET /leaderboard?filter=&page=&per_page= then cache the result. */
    public void fetch(final String filter, final int page, final Callback callback) {
        api.leaderboard(filter, page, 20).enqueue(new retrofit2.Callback<LeaderboardEnvelope>() {
            @Override
            public void onResponse(@NonNull Call<LeaderboardEnvelope> call,
                                   @NonNull Response<LeaderboardEnvelope> response) {
                LeaderboardEnvelope envelope = response.body();
                if (response.isSuccessful() && envelope != null && envelope.isSuccess()) {
                    cache(filter, page, envelope);
                    if (callback != null) callback.onSuccess(envelope);
                } else if (response.code() == 401) {
                    if (callback != null) callback.onAuthError();
                } else {
                    if (callback != null) callback.onError(extractError(response));
                }
            }

            @Override
            public void onFailure(@NonNull Call<LeaderboardEnvelope> call, @NonNull Throwable t) {
                Log.e(TAG, "fetch failed", t);
                if (callback != null) callback.onError(t.getMessage() == null ? "Network error" : t.getMessage());
            }
        });
    }

    /** Cached snapshot for a filter period, or null. */
    public RinjoraLeaderboardSnapshot getCached(String filter) {
        try {
            Box<RinjoraLeaderboardSnapshot> box =
                    ((MyApp) context.getApplicationContext()).getBoxStore().boxFor(RinjoraLeaderboardSnapshot.class);
            for (RinjoraLeaderboardSnapshot s : box.query()
                    .orderDesc(RinjoraLeaderboardSnapshot_.fetchedAt).build().find()) {
                if (filter == null ? s.getFilter() == null : filter.equals(s.getFilter())) {
                    return s;
                }
            }
        } catch (Exception e) {
            // fall through
        }
        return null;
    }

    /** Re-parse the cached envelope JSON for offline rendering, or null. */
    public LeaderboardEnvelope getCachedEnvelope(String filter) {
        RinjoraLeaderboardSnapshot snap = getCached(filter);
        if (snap == null || snap.getRawJson() == null) return null;
        try {
            return gson.fromJson(snap.getRawJson(), LeaderboardEnvelope.class);
        } catch (RuntimeException e) {
            return null;
        }
    }

    private void cache(String filter, int page, LeaderboardEnvelope envelope) {
        RinjoraLeaderboardSnapshot s = new RinjoraLeaderboardSnapshot();
        s.setFilter(filter);
        s.setCurrentPage(page);
        s.setFetchedAt(System.currentTimeMillis());
        if (envelope.getMeta() != null) {
            s.setLastPage(envelope.getMeta().getLastPage());
            s.setTotal(envelope.getMeta().getTotal());
            s.setCurrentPage(envelope.getMeta().getCurrentPage());
        }
        if (envelope.getMe() != null) {
            s.setHasMe(true);
            s.setMeName(envelope.getMe().getName());
            s.setMeRank(envelope.getMe().getRank());
            s.setMePoints(envelope.getMe().getPoints());
            s.setMeTotalPlayers(envelope.getMe().getTotalPlayers());
            s.setMePercentile(envelope.getMe().getPercentile());
        }
        s.setRawJson(gson.toJson(envelope));

        Box<RinjoraLeaderboardSnapshot> box =
                ((MyApp) context.getApplicationContext()).getBoxStore().boxFor(RinjoraLeaderboardSnapshot.class);
        box.put(s);
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
}
