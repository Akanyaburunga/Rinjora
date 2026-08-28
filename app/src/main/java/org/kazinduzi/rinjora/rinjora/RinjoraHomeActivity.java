package org.kazinduzi.rinjora.rinjora;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Toast;

import java.util.Locale;

import org.kazinduzi.rinjora.BaseActivity;
import org.kazinduzi.rinjora.R;
import org.kazinduzi.rinjora.data.RinjoraAuthRepository;
import org.kazinduzi.rinjora.data.RinjoraSummaryRepository;
import org.kazinduzi.rinjora.databinding.ActivityRinjoraHomeBinding;
import org.kazinduzi.rinjora.entities.RinjoraSummarySnapshot;
import org.kazinduzi.rinjora.network.AuthTokenStore;

/**
 * Rinjora Home screen backed by {@code GET /me/summary} (plan §4.1), following the
 * offline-first pattern: render from cache instantly, then refresh in the background,
 * and show a staleness line.
 *
 * This is the authenticated landing screen. "Play riddles" opens the core game loop
 * in {@link RinjoraPlayActivity}.
 */
public class RinjoraHomeActivity extends BaseActivity {

    private ActivityRinjoraHomeBinding binding;
    private RinjoraSummaryRepository repository;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable pollTask = new Runnable() {
        @Override
        public void run() {
            if (binding != null) {
                refresh(false);
                handler.postDelayed(this, 60_000);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRinjoraHomeBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        repository = new RinjoraSummaryRepository(this);

        if (!AuthTokenStore.get(this).hasValidToken()) {
            goToAuth();
            return;
        }

        binding.btnLogout.setOnClickListener(v -> logout());
        binding.btnRiddlesDebug.setOnClickListener(v ->
                startActivity(new Intent(this, RinjoraPlayActivity.class)));
        binding.btnDaily.setOnClickListener(v ->
                startActivity(new Intent(this, RinjoraDailyActivity.class)));
        binding.btnLeaderboard.setOnClickListener(v ->
                startActivity(new Intent(this, RinjoraLeaderboardActivity.class)));
        binding.btnFavorites.setOnClickListener(v ->
                startActivity(new Intent(this, RinjoraFavoritesActivity.class)));
        binding.btnAchievements.setOnClickListener(v ->
                startActivity(new Intent(this, RinjoraAchievementsActivity.class)));
        binding.btnDuels.setOnClickListener(v ->
                startActivity(new Intent(this, RinjoraDuelsActivity.class)));
        binding.btnContribute.setOnClickListener(v ->
                startActivity(new Intent(this, RinjoraContributeActivity.class)));
        binding.btnSubmissions.setOnClickListener(v ->
                startActivity(new Intent(this, RinjoraSubmissionsActivity.class)));

        renderCached();
        refresh(!hasCache());
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Plan §4.1 recommends auto-refreshing the summary on resume.
        renderCached();
        refresh(false);
        handler.removeCallbacks(pollTask);
        handler.postDelayed(pollTask, 60_000);
    }

    @Override
    protected void onPause() {
        super.onPause();
        handler.removeCallbacks(pollTask);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacks(pollTask);
        binding = null;
    }

    private boolean hasCache() {
        return repository.getCached() != null;
    }

    /** Render from the ObjectBox cache synchronously (no network required). */
    private void renderCached() {
        RinjoraSummarySnapshot s = repository.getCached();
        if (s == null) {
            binding.tvGreeting.setText("Hello,");
            binding.tvName.setText("Player");
            binding.tvSyncStatus.setText("Tap refresh when online to load your profile.");
            return;
        }
        binding.tvGreeting.setText("Hello,");
        binding.tvName.setText(emptyTo(s.getName(), "Player"));
        binding.tvLevel.setText("Level " + s.getLevel());
        binding.tvReputation.setText(s.getReputation() + " reputation");
        binding.tvStreak.setText(String.valueOf(s.getCurrentStreak()));
        binding.tvSolved.setText(String.valueOf(s.getRiddlesSolved()));
        binding.tvBadges.setText(s.getEarnedBadges() + "/" + s.getTotalBadges());

        binding.tvActivity.setText(String.format(Locale.getDefault(),
                "• %,d attempts\n• %,d unique\n• %,d%% accurate\n• %,d contributions\n• %,d shares",
                s.getTotalAttempts(),
                s.getUniqueRiddles(),
                (int) Math.round(s.getAccuracy()),
                s.getSubmissionsCount(),
                s.getSharesCount()));

        long staleness = repository.getStalenessMs();
        binding.tvSyncStatus.setText(staleness < 0 ? "Not synced yet"
                : "Updated " + formatStaleness(staleness));
    }

    /** Fetch fresh data; show the spinner only when there is nothing cached yet. */
    private void refresh(final boolean showSpinner) {
        if (showSpinner) {
            binding.progressBar.setVisibility(View.VISIBLE);
        }
        repository.fetch(new RinjoraSummaryRepository.Callback() {
            @Override
            public void onSuccess(RinjoraSummarySnapshot snapshot) {
                binding.progressBar.setVisibility(View.GONE);
                renderCached();
            }

            @Override
            public void onAuthError() {
                binding.progressBar.setVisibility(View.GONE);
                Toast.makeText(RinjoraHomeActivity.this, "Session expired. Log in again.",
                        Toast.LENGTH_SHORT).show();
                goToAuth();
            }

            @Override
            public void onError(String message) {
                binding.progressBar.setVisibility(View.GONE);
                if (!hasCache()) {
                    binding.tvSyncStatus.setText("Couldn’t load your profile: " + message);
                }
            }
        });
    }

    private void logout() {
        new RinjoraAuthRepository(this)
                .logout(new RinjoraAuthRepository.AuthCallback() {
                    @Override
                    public void onSuccess() {
                        goToAuth();
                    }

                    @Override
                    public void onError(String message) {
                        goToAuth();
                    }
                });
    }

    private void goToAuth() {
        Intent intent = new Intent(RinjoraHomeActivity.this, RinjoraAuthActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private String emptyTo(String value, String fallback) {
        return (value == null || value.trim().isEmpty()) ? fallback : value;
    }

    /** "just now / 5m ago / 3h ago / 2d ago / date" from a staleness duration in ms. */
    private String formatStaleness(long ms) {
        long a = Math.abs(ms);
        if (a < 60_000) return "just now";
        if (a < 3_600_000) return (a / 60_000) + "m ago";
        if (a < 86_400_000) return (a / 3_600_000) + "h ago";
        if (a < 7L * 86_400_000) return (a / 86_400_000) + "d ago";
        return java.text.DateFormat.getDateInstance().format(
                new java.util.Date(System.currentTimeMillis() - ms));
    }
}
