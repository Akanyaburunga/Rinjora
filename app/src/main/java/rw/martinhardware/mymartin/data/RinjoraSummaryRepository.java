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
import rw.martinhardware.mymartin.entities.RinjoraSummarySnapshot;
import rw.martinhardware.mymartin.entities.RinjoraSummarySnapshot_;
import rw.martinhardware.mymartin.network.ApiEnvelope;
import rw.martinhardware.mymartin.network.AuthTokenStore;
import rw.martinhardware.mymartin.network.RinjoraApi;
import rw.martinhardware.mymartin.network.RinjoraApiClient;
import rw.martinhardware.mymartin.network.dto.SummaryDto;

/**
 * Offline-first repository for the Rinjora Home screen ({@code GET /me/summary}).
 *
 * Mirrors the driver pattern in {@link DriverHomeRepository}: every fetch writes a
 * {@link RinjoraSummarySnapshot} to ObjectBox before the UI reads it, so Home renders
 * instantly from cache even fully offline, then refreshes in the background.
 */
public class RinjoraSummaryRepository {

    private static final String TAG = "RinjoraSummaryRepository";
    private final Gson gson = new Gson();

    public interface Callback {
        void onSuccess(RinjoraSummarySnapshot snapshot);

        void onAuthError();

        void onError(String message);
    }

    private final Context context;
    private final RinjoraApi api;

    public RinjoraSummaryRepository(Context context) {
        this.context = context.getApplicationContext();
        this.api = RinjoraApiClient.get(context).api();
    }

    /** GET /me/summary, then cache the result. */
    public void fetch(final RinjoraSummaryRepository.Callback callback) {
        api.summary().enqueue(new retrofit2.Callback<ApiEnvelope<SummaryDto>>() {
            @Override
            public void onResponse(@NonNull retrofit2.Call<ApiEnvelope<SummaryDto>> call,
                                   @NonNull retrofit2.Response<ApiEnvelope<SummaryDto>> response) {
                ApiEnvelope<SummaryDto> envelope = response.body();
                if (response.isSuccessful() && envelope != null && envelope.isSuccess()
                        && envelope.getData() != null) {
                    RinjoraSummarySnapshot snapshot = toEntity(envelope.getData());
                    snapshot.setFetchedAt(System.currentTimeMillis());
                    save(snapshot);
                    if (callback != null) callback.onSuccess(snapshot);
                } else if (response.code() == 401) {
                    if (callback != null) callback.onAuthError();
                } else {
                    if (callback != null) callback.onError(extractError(response, envelope));
                }
            }

            @Override
            public void onFailure(@NonNull Call<ApiEnvelope<SummaryDto>> call, @NonNull Throwable t) {
                Log.e(TAG, "fetch failed", t);
                if (callback != null) callback.onError(t.getMessage() == null ? "Network error" : t.getMessage());
            }
        });
    }

    /** Latest cached snapshot (by last sync), or null when never synced. */
    public RinjoraSummarySnapshot getCached() {
        try {
            Box<RinjoraSummarySnapshot> box =
                    ((MyApp) context.getApplicationContext()).getBoxStore().boxFor(RinjoraSummarySnapshot.class);
            return box.query().orderDesc(RinjoraSummarySnapshot_.fetchedAt).build().findFirst();
        } catch (Exception e) {
            return null;
        }
    }

    /** How old the cached snapshot is, in ms. -1 when there is no cache. */
    public long getStalenessMs() {
        RinjoraSummarySnapshot snap = getCached();
        return snap != null ? System.currentTimeMillis() - snap.getFetchedAt() : -1;
    }

    private void save(RinjoraSummarySnapshot snapshot) {
        Box<RinjoraSummarySnapshot> box =
                ((MyApp) context.getApplicationContext()).getBoxStore().boxFor(RinjoraSummarySnapshot.class);
        // Keep one row per user: remove any earlier row for the same user.
        for (RinjoraSummarySnapshot old : box.query()
                .equal(RinjoraSummarySnapshot_.userId, snapshot.getUserId()).build().find()) {
            if (old.id != snapshot.id) box.remove(old);
        }
        box.put(snapshot);
    }

    private RinjoraSummarySnapshot toEntity(SummaryDto dto) {
        RinjoraSummarySnapshot s = new RinjoraSummarySnapshot();

        if (dto.getUser() != null) {
            s.setUserId(dto.getUser().getId());
            s.setName(dto.getUser().getName());
            s.setProfilePictureUrl(dto.getUser().getProfilePictureUrl());
        } else {
            s.setUserId(AuthTokenStore.get(context).getUserId());
        }

        if (dto.getPoints() != null) {
            s.setReputation(dto.getPoints().getReputation());
            s.setLevel(dto.getPoints().getLevel());
        }
        if (dto.getStreak() != null) {
            s.setCurrentStreak(dto.getStreak().getCurrent());
            s.setLongestStreak(dto.getStreak().getLongest());
        }
        if (dto.getBadges() != null) {
            s.setEarnedBadges(dto.getBadges().getEarnedCount());
            s.setTotalBadges(dto.getBadges().getTotal());
        }
        s.setFavoritesCount(dto.getFavoritesCount());

        if (dto.getActivity() != null) {
            s.setTotalAttempts(dto.getActivity().getTotalAttempts());
            s.setRiddlesSolved(dto.getActivity().getRiddlesSolved());
            s.setAccuracy(dto.getActivity().getAccuracy());
            s.setUniqueRiddles(dto.getActivity().getUniqueRiddles());
            s.setSubmissionsCount(dto.getActivity().getSubmissionsCount());
            s.setSharesCount(dto.getActivity().getSharesCount());
        }

        s.setRawJson(gson.toJson(dto));
        return s;
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
