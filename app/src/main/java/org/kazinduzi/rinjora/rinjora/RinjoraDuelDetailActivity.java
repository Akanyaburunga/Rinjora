package org.kazinduzi.rinjora.rinjora;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import org.kazinduzi.rinjora.R;
import org.kazinduzi.rinjora.databinding.ActivityRinjoraDuelDetailBinding;
import org.kazinduzi.rinjora.data.RinjoraDuelRepository;
import org.kazinduzi.rinjora.network.AuthTokenStore;
import org.kazinduzi.rinjora.network.dto.DuelDto;
import org.kazinduzi.rinjora.network.dto.DuelSolveResponseDto;
import org.kazinduzi.rinjora.network.dto.DuelUserDto;

/**
 * Rinjora live duel screen (plan Phase I, §7.3/§7.6): shows the current status of a
 * duel via {@code GET /duels/{id}}, lets an accepted duel's player submit a single
 * answer ({@code POST /duels/{id}/solve}), then polls until it resolves and shows the
 * winner + reputation delta. The opponent's answer is never shown (anti-cheat).
 */
public class RinjoraDuelDetailActivity extends AppCompatActivity {

    private ActivityRinjoraDuelDetailBinding binding;
    private RinjoraDuelRepository repository;

    private long duelId;
    private String direction;
    private boolean myMoved;
    private DuelDto current;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable poller = new Runnable() {
        @Override
        public void run() {
            refresh();
            if (current != null && "accepted".equals(current.getStatus())) {
                handler.postDelayed(this, 5000);
            }
        }
    };
    private boolean polling;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRinjoraDuelDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        if (!AuthTokenStore.get(this).hasValidToken()) {
            goToAuth();
            return;
        }

        duelId = getIntent().getLongExtra("duel_id", -1);
        direction = getIntent().getStringExtra("direction");
        if (duelId <= 0) {
            finish();
            return;
        }

        repository = new RinjoraDuelRepository(this);

        binding.btnRefresh.setOnClickListener(v -> refresh());
        binding.btnSubmit.setOnClickListener(v -> submit());

        refresh();
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }

    private void refresh() {
        binding.progressBar.setVisibility(View.VISIBLE);
        repository.fetchDuel(duelId, new RinjoraDuelRepository.Callback<DuelDto>() {
            @Override
            public void onSuccess(DuelDto duel) {
                binding.progressBar.setVisibility(View.GONE);
                current = duel;
                render(duel);
            }

            @Override
            public void onAuthError() {
                binding.progressBar.setVisibility(View.GONE);
                goToAuth();
            }

            @Override
            public void onError(String message) {
                binding.progressBar.setVisibility(View.GONE);
                Toast.makeText(RinjoraDuelDetailActivity.this, message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void render(DuelDto duel) {
        DuelUserDto other = "outgoing".equals(direction) ? duel.getOpponent() : duel.getInitiator();
        String otherName = other != null && other.getName() != null ? other.getName() : "?";
        binding.tvVs.setText("vs " + otherName);

        StringBuilder meta = new StringBuilder();
        meta.append("Wager ").append(duel.getWager());
        String created = duel.getCreatedAt();
        if (created != null && !created.isEmpty()) {
            meta.append(" · ").append(String.format(java.util.Locale.getDefault(),
                    "%tD", parseDate(created)));
        }
        binding.tvMeta.setText(meta.toString());

        String question = duel.getRiddle() != null && duel.getRiddle().getQuestion() != null
                ? duel.getRiddle().getQuestion() : "Riddle";
        binding.tvRiddle.setText(question);

        String status = duel.getStatus() != null ? duel.getStatus() : "";
        binding.tvStatus.setText(status.toUpperCase(java.util.Locale.ROOT));

        binding.resultCard.setVisibility(View.GONE);
        binding.tvWaiting.setVisibility(View.GONE);
        binding.answerWrap.setVisibility(View.GONE);
        binding.btnSubmit.setVisibility(View.GONE);

        switch (status) {
            case "accepted":
                if (myMoved) {
                    showWaiting("Answer submitted — waiting on your opponent…");
                } else {
                    binding.answerWrap.setVisibility(View.VISIBLE);
                    binding.btnSubmit.setVisibility(View.VISIBLE);
                }
                startPolling();
                break;
            case "completed":
                stopPolling();
                showWinner(duel, otherName);
                break;
            case "pending":
                stopPolling();
                showWaiting(duel.isOutgoing()
                        ? "Waiting for the opponent to accept…"
                        : "This duel has not started yet.");
                break;
            default:
                stopPolling();
                showWaiting(status.toUpperCase(java.util.Locale.ROOT));
                break;
        }
    }

    private void submit() {
        String answer = binding.etAnswer.getText().toString().trim();
        if (answer.isEmpty()) {
            Toast.makeText(this, "Type your answer first.", Toast.LENGTH_SHORT).show();
            return;
        }
        binding.progressBar.setVisibility(View.VISIBLE);
        binding.btnSubmit.setEnabled(false);
        repository.solve(duelId, answer, new RinjoraDuelRepository.Callback<DuelSolveResponseDto>() {
            @Override
            public void onSuccess(DuelSolveResponseDto result) {
                binding.progressBar.setVisibility(View.GONE);
                binding.btnSubmit.setEnabled(true);
                myMoved = true;

                binding.resultCard.setVisibility(View.VISIBLE);
                Boolean correct = result.getCorrect();
                binding.tvResultTitle.setText(Boolean.TRUE.equals(correct) ? "Correct!" : "Not quite");
                String body = result.getMessage() != null ? result.getMessage() : "";
                if (Boolean.TRUE.equals(correct) && result.getAnswer() != null) {
                    if (!body.isEmpty()) body += "\n\n";
                    body += "Answer: “" + result.getAnswer() + "”";
                }
                binding.tvResultBody.setText(body);

                binding.answerWrap.setVisibility(View.GONE);
                binding.btnSubmit.setVisibility(View.GONE);

                if (result.isResolved()) {
                    stopPolling();
                    showWaiting("Duel resolved — refreshing…");
                    handler.postDelayed(refreshOnce, 1500);
                } else {
                    showWaiting("Answer submitted — waiting on your opponent…");
                    startPolling();
                }
            }

            @Override
            public void onAuthError() {
                binding.progressBar.setVisibility(View.GONE);
                binding.btnSubmit.setEnabled(true);
                goToAuth();
            }

            @Override
            public void onError(String message) {
                binding.progressBar.setVisibility(View.GONE);
                binding.btnSubmit.setEnabled(true);
                Toast.makeText(RinjoraDuelDetailActivity.this, message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private final Runnable refreshOnce = new Runnable() {
        @Override
        public void run() {
            refresh();
        }
    };

    private void showWaiting(String text) {
        binding.tvWaiting.setVisibility(View.VISIBLE);
        binding.tvWaiting.setText(text);
    }

    private void showWinner(DuelDto duel, String otherName) {
        Long winnerId = duel.getWinnerId();
        DuelUserDto other = "outgoing".equals(direction) ? duel.getOpponent() : duel.getInitiator();
        binding.resultCard.setVisibility(View.VISIBLE);
        if (winnerId == null || winnerId == 0) {
            binding.tvResultTitle.setText("Resolved");
            binding.tvResultBody.setText("No winner — duel voided.");
        } else if (other != null && other.getId() == winnerId) {
            binding.tvResultTitle.setText(otherName + " won");
            binding.tvResultBody.setText("They take the +" + duel.getWager() + " reputation.");
        } else {
            binding.tvResultTitle.setText("You won!");
            binding.tvResultBody.setText("You take the +" + duel.getWager() + " reputation.");
            binding.tvResultTitle.setTextColor(ContextCompat.getColor(this, R.color.brand_success));
        }
    }

    private void startPolling() {
        if (!polling) {
            polling = true;
            handler.postDelayed(poller, 5000);
        }
    }

    private void stopPolling() {
        polling = false;
        handler.removeCallbacks(poller);
    }

    private java.util.Date parseDate(String dateStr) {
        if (dateStr == null) return new java.util.Date();
        try {
            return new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss",
                    java.util.Locale.US).parse(dateStr.replace("Z", ""));
        } catch (Exception e) {
            return new java.util.Date();
        }
    }

    private void goToAuth() {
        Intent intent = new Intent(this, RinjoraAuthActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
