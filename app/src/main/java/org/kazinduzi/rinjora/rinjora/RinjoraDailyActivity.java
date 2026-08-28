package org.kazinduzi.rinjora.rinjora;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.text.DateFormat;
import java.util.Date;

import org.kazinduzi.rinjora.R;
import org.kazinduzi.rinjora.data.RinjoraDailyRepository;
import org.kazinduzi.rinjora.databinding.ActivityRinjoraDailyBinding;
import org.kazinduzi.rinjora.entities.RinjoraDailySnapshot;
import org.kazinduzi.rinjora.network.AuthTokenStore;
import org.kazinduzi.rinjora.network.dto.FreezeResponseDto;

/**
 * Rinjora Daily riddle hub (plan Phase E, §2.4–§2.6, §2.12): shows today's streak,
 * a streak-at-risk warning, pending-challenges badge, and today's daily riddle.
 * The actual solving reuses the Phase 5 play screen ({@link RinjoraPlayRiddleActivity});
 * this screen supplies the daily gating and the streak-freeze action.
 */
public class RinjoraDailyActivity extends AppCompatActivity {

    private ActivityRinjoraDailyBinding binding;
    private RinjoraDailyRepository repository;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable refreshTask = new Runnable() {
        @Override
        public void run() {
            if (binding != null) {
                refresh();
                handler.postDelayed(this, 60_000);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRinjoraDailyBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        repository = new RinjoraDailyRepository(this);

        if (!AuthTokenStore.get(this).hasValidToken()) {
            goToAuth();
            return;
        }

        binding.btnRefresh.setOnClickListener(v -> refresh());
        binding.btnSolve.setOnClickListener(v -> openTodayRiddle());
        binding.btnFreeze.setOnClickListener(v -> spendFreeze());

        renderCached();
        refresh();
    }

    @Override
    protected void onResume() {
        super.onResume();
        renderCached();
        refresh();
        handler.removeCallbacks(refreshTask);
        handler.postDelayed(refreshTask, 60_000);
    }

    @Override
    protected void onPause() {
        super.onPause();
        handler.removeCallbacks(refreshTask);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacks(refreshTask);
        binding = null;
    }

    private void renderCached() {
        binding.tvDate.setText(DateFormat.getDateInstance().format(new Date()));
        RinjoraDailySnapshot s = repository.getCachedForToday();
        if (s == null) {
            binding.tvStreakValue.setText("0");
            binding.tvBestStreak.setText("longest 0");
            binding.tvQuestion.setText("Loading today's riddle…");
            binding.btnSolve.setEnabled(false);
            return;
        }
        binding.tvStreakValue.setText(String.valueOf(s.getCurrentStreak()));
        binding.tvBestStreak.setText("longest " + s.getLongestStreak());
        renderRiddle(s);
        renderRisk(s);
        renderPending(s);
    }

    private void renderRiddle(RinjoraDailySnapshot s) {
        if (s.isDailySolved()) {
            binding.tvQuestion.setText("You solved today's riddle. Come back tomorrow!");
            binding.btnSolve.setEnabled(false);
            binding.btnSolve.setText("Solved today");
        } else if (s.getDailyQuestion() != null) {
            binding.tvQuestion.setText(s.getDailyQuestion());
            binding.btnSolve.setEnabled(s.isDailyAvailable());
            binding.btnSolve.setText("Solve today's riddle");
        } else {
            binding.tvQuestion.setText("Loading today's riddle…");
            binding.btnSolve.setEnabled(false);
        }
    }

    private void renderRisk(RinjoraDailySnapshot s) {
        binding.atRiskCard.setVisibility(s.isStreakAtRisk() ? View.VISIBLE : View.GONE);
        binding.btnFreeze.setEnabled(!s.isDailySolved());
    }

    private void renderPending(RinjoraDailySnapshot s) {
        if (s.getPendingChallenges() > 0) {
            binding.tvPending.setText("⚔  " + s.getPendingChallenges() + " pending challenge(s)");
            binding.tvPending.setVisibility(View.VISIBLE);
        } else {
            binding.tvPending.setVisibility(View.GONE);
        }
    }

    private void refresh() {
        binding.progressBar.setVisibility(View.VISIBLE);
        repository.load(new RinjoraDailyRepository.Callback<RinjoraDailySnapshot>() {
            @Override
            public void onSuccess(RinjoraDailySnapshot snapshot) {
                if (binding == null) return;
                binding.progressBar.setVisibility(View.GONE);
                renderCached();
            }

            @Override
            public void onAuthError() {
                if (binding == null) return;
                binding.progressBar.setVisibility(View.GONE);
                goToAuth();
            }

            @Override
            public void onError(String message) {
                if (binding == null) return;
                binding.progressBar.setVisibility(View.GONE);
                Toast.makeText(RinjoraDailyActivity.this, message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void openTodayRiddle() {
        RinjoraDailySnapshot s = repository.getCachedForToday();
        if (s == null || s.getDailyRiddleId() <= 0) {
            Toast.makeText(this, "Daily riddle isn't available yet.", Toast.LENGTH_SHORT).show();
            return;
        }
        Intent i = new Intent(this, RinjoraPlayRiddleActivity.class);
        i.putExtra("riddle_id", s.getDailyRiddleId());
        startActivity(i);
    }

    private void spendFreeze() {
        binding.progressBar.setVisibility(View.VISIBLE);
        binding.btnFreeze.setEnabled(false);
        repository.freeze(new RinjoraDailyRepository.Callback<FreezeResponseDto>() {
            @Override
            public void onSuccess(FreezeResponseDto result) {
                if (binding == null) return;
                binding.progressBar.setVisibility(View.GONE);
                binding.btnFreeze.setEnabled(true);
                Toast.makeText(RinjoraDailyActivity.this,
                        result.isFreezeActive() ? "Streak frozen for today."
                                : result.getFreezesRemaining() + " freeze(s) left.",
                        Toast.LENGTH_SHORT).show();
                renderCached();
            }

            @Override
            public void onAuthError() {
                if (binding == null) return;
                binding.progressBar.setVisibility(View.GONE);
                binding.btnFreeze.setEnabled(true);
                goToAuth();
            }

            @Override
            public void onError(String message) {
                if (binding == null) return;
                binding.progressBar.setVisibility(View.GONE);
                binding.btnFreeze.setEnabled(true);
                Toast.makeText(RinjoraDailyActivity.this, message, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void goToAuth() {
        Intent intent = new Intent(this, RinjoraAuthActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
