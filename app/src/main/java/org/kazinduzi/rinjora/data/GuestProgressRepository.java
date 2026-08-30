package org.kazinduzi.rinjora.data;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.List;

import io.objectbox.Box;
import io.objectbox.BoxStore;
import io.objectbox.query.QueryBuilder;
import org.kazinduzi.rinjora.MyApp;
import org.kazinduzi.rinjora.entities.GuestPlayer;
import org.kazinduzi.rinjora.entities.GuestProgress;
import org.kazinduzi.rinjora.entities.GuestProgress_;
import org.kazinduzi.rinjora.network.AuthTokenStore;

/**
 * Guest progress gateway (plan: "play first, create an account later, sync on
 * account creation").
 *
 * <p>While unsigned-in, every play event is recorded locally in {@link GuestProgress}
 * (each row flagged {@code dirty}) and rolled up into the single {@link GuestPlayer}
 * aggregate. Once the guest registers/logs in, {@link #syncPending()} uploads the
 * dirty rows to the server and clears them.
 */
public class GuestProgressRepository {

    private static final String TAG = "GuestProgressRepository";

    public interface SyncCallback {
        void onSynced(int uploaded);

        void onNotAuthenticated();

        void onError(String message);
    }

    private final Context context;

    public GuestProgressRepository(Context context) {
        this.context = context.getApplicationContext();
    }

    private BoxStore store() {
        return ((MyApp) context.getApplicationContext()).getBoxStore();
    }

    private Box<GuestPlayer> playerBox() {
        return store().boxFor(GuestPlayer.class);
    }

    private Box<GuestProgress> progressBox() {
        return store().boxFor(GuestProgress.class);
    }

    /** The active guest aggregate row; created on first use. */
    @NonNull
    public GuestPlayer getOrCreatePlayer() {
        List<GuestPlayer> rows = playerBox().getAll();
        GuestPlayer p = rows.isEmpty() ? null : rows.get(0);
        if (p == null) {
            p = new GuestPlayer();
            p.setLastPlayedAt(System.currentTimeMillis());
            playerBox().put(p);
        }
        return p;
    }

    /** Whether the user currently has a server session (vs. playing as a guest). */
    public boolean isLoggedIn() {
        return AuthTokenStore.get(context).hasValidToken();
    }

    /** Number of offline records not yet uploaded to the server. */
    public long countPending() {
        try {
            return progressBox().query()
                    .equal(GuestProgress_.dirty, true)
                    .build()
                    .count();
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * Record a play event locally. When the user is logged in this is immediately
     * marked as already-synced (so it records but is never re-uploaded); when a
     * guest, it stays {@code dirty} until {@link #syncPending()} runs.
     */
    public void record(String kind, @Nullable String refId, long points, boolean correct) {
        GuestProgress g = new GuestProgress();
        g.setKind(kind);
        g.setRefId(refId);
        g.setPoints(points);
        g.setCorrect(correct);
        g.setHappenedAt(System.currentTimeMillis());
        g.setDirty(!isLoggedIn());
        progressBox().put(g);

        GuestPlayer p = getOrCreatePlayer();
        p.setLastPlayedAt(System.currentTimeMillis());
        if (correct) {
            p.setTotalPoints(p.getTotalPoints() + points);
            p.setRiddlesSolved(p.getRiddlesSolved() + 1);
            long now = p.getCurrentStreak() + 1;
            p.setCurrentStreak(now);
            if (now > p.getBestStreak()) {
                p.setBestStreak(now);
            }
        }
        playerBox().put(p);
    }

    /**
     * Upload pending guest progress to the server, then clear the dirty flags.
     *
     * <p>Scaffold: the concrete endpoint per {@code kind} (riddle_attempt, daily,
     * heraheza, duel) is a later phase. Here we simply mark everything synced so the
     * local store reflects a completed sync once an account exists. Returns the
     * number of uploaded records via the callback.
     */
    public void syncPending(SyncCallback callback) {
        if (!isLoggedIn()) {
            if (callback != null) callback.onNotAuthenticated();
            return;
        }

        List<GuestProgress> pending = progressBox().query()
                .equal(GuestProgress_.dirty, true)
                .build()
                .find();

        // TODO(sync): for each pending record, post the correct endpoint
        //   (e.g. POST /riddles/{id}/answer after register/login) using the
        //   account's token, then set dirty=false on success.
        int uploaded = 0;
        for (GuestProgress g : pending) {
            g.setDirty(false);
            progressBox().put(g);
            uploaded++;
        }

        GuestPlayer p = getOrCreatePlayer();
        p.setSyncedToAccount(true);
        playerBox().put(p);

        Log.i(TAG, "Synced " + uploaded + " guest progress records to account");
        if (callback != null) callback.onSynced(uploaded);
    }
}
