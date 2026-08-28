package rw.martinhardware.mymartin.rinjora;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import rw.martinhardware.mymartin.R;
import rw.martinhardware.mymartin.data.RinjoraRiddleRepository;
import rw.martinhardware.mymartin.databinding.ActivityRinjoraPlayRiddleBinding;
import rw.martinhardware.mymartin.entities.RinjoraRiddleSnapshot;
import rw.martinhardware.mymartin.network.AuthTokenStore;
import rw.martinhardware.mymartin.network.dto.AnswerResponseDto;
import rw.martinhardware.mymartin.network.dto.HintDto;
import rw.martinhardware.mymartin.network.dto.RevealDto;

/**
 * Rinjora single-riddle play screen (plan Phase D §2.2/§2.8/§2.9): shows the
 * question, progressively reveals hints, accepts an answer via
 * {@code POST /riddles/{id}/answer}, and offers a no-reward reveal/learning mode.
 * <p>
 * The confidential {@code answer} is only ever shown after a correct solve or an
 * explicit reveal — it is never cached to ObjectBox.
 */
public class RinjoraPlayRiddleActivity extends AppCompatActivity {

    private ActivityRinjoraPlayRiddleBinding binding;
    private RinjoraRiddleRepository repository;

    private long riddleId;
    private int hintsRevealed;
    private String hint1;
    private String hint2;
    private boolean solved;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRinjoraPlayRiddleBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        repository = new RinjoraRiddleRepository(this);

        if (!AuthTokenStore.get(this).hasValidToken()) {
            goToAuth();
            return;
        }

        riddleId = getIntent().getLongExtra("riddle_id", -1);
        if (riddleId <= 0) {
            finish();
            return;
        }

        binding.btnLogout.setOnClickListener(v -> logout());
        binding.btnBackToList.setOnClickListener(v -> finish());
        binding.btnHint.setOnClickListener(v -> revealHint());
        binding.btnAnswer.setOnClickListener(v -> submitAnswer());
        binding.btnReveal.setOnClickListener(v -> revealAnswer());

        renderCached();
        refresh();
    }

    private boolean hasCache() {
        return repository.getCached(riddleId) != null;
    }

    private void renderCached() {
        RinjoraRiddleSnapshot cache = repository.getCached(riddleId);
        if (cache != null) {
            apply(cache.getQuestion(), cache.getDifficulty(), cache.getRiddleType(),
                    cache.getCategoryName(), cache.getHintsRevealed(), cache.isSolved(),
                    null, null);
        } else {
            binding.tvQuestion.setText("Loading riddle…");
            binding.tvMeta.setText("");
            binding.tvCategory.setText("Riddle");
        }
    }

    private void refresh() {
        binding.progressBar.setVisibility(View.VISIBLE);
        repository.fetchRiddle(riddleId, new RinjoraRiddleRepository.Callback<RinjoraRiddleRepository.RiddleBundle>() {
            @Override
            public void onSuccess(RinjoraRiddleRepository.RiddleBundle bundle) {
                binding.progressBar.setVisibility(View.GONE);
                RinjoraRiddleSnapshot s = bundle.snapshot;
                apply(s.getQuestion(), s.getDifficulty(), s.getRiddleType(),
                        s.getCategoryName(), s.getHintsRevealed(), s.isSolved(),
                        bundle.hint1, bundle.hint2);
            }

            @Override
            public void onAuthError() {
                binding.progressBar.setVisibility(View.GONE);
                goToAuth();
            }

            @Override
            public void onError(String message) {
                binding.progressBar.setVisibility(View.GONE);
                if (!hasCache()) {
                    binding.tvQuestion.setText("Couldn’t load riddle: " + message);
                } else {
                    Toast.makeText(RinjoraPlayRiddleActivity.this, message, Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void apply(String question, String difficulty, String type, String category,
                       int hintsRevealed, boolean solved, String h1, String h2) {
        binding.tvQuestion.setText(question);
        StringBuilder meta = new StringBuilder();
        if (difficulty != null && !difficulty.isEmpty()) meta.append(cap(difficulty));
        if (type != null && !type.isEmpty()) {
            if (meta.length() > 0) meta.append(" · ");
            meta.append(cap(type.replace("_", " ")));
        }
        binding.tvMeta.setText(meta.toString());
        binding.tvCategory.setText(category != null ? category : "Riddle");

        if (h1 != null) this.hint1 = h1;
        if (h2 != null) this.hint2 = h2;
        this.hintsRevealed = hintsRevealed;
        this.solved = solved;

        renderHints();
        setLocked(solved);
        if (solved) {
            binding.tvResultTitle.setText("Solved");
            binding.tvResultBody.setText("You already solved this riddle.");
            binding.resultCard.setVisibility(View.VISIBLE);
        }
    }

    private void renderHints() {
        if (this.hint1 == null) {
            binding.tvHintsLabel.setVisibility(View.GONE);
            binding.tvHint1.setVisibility(View.GONE);
            binding.tvHint2.setVisibility(View.GONE);
        } else {
            binding.tvHintsLabel.setVisibility(View.VISIBLE);
            if (hintsRevealed >= 1) {
                binding.tvHint1.setText("• " + hint1);
                binding.tvHint1.setVisibility(View.VISIBLE);
            }
            if (hintsRevealed >= 2 && hint2 != null) {
                binding.tvHint2.setText("• " + hint2);
                binding.tvHint2.setVisibility(View.VISIBLE);
            }
            boolean more = hint2 != null ? hintsRevealed < 2 : hintsRevealed < 1;
            binding.btnHint.setVisibility(more && !solved ? View.VISIBLE : View.GONE);
        }
    }

    private void revealHint() {
        binding.progressBar.setVisibility(View.VISIBLE);
        binding.btnHint.setEnabled(false);
        repository.requestHint(riddleId, new RinjoraRiddleRepository.Callback<HintDto>() {
            @Override
            public void onSuccess(HintDto hint) {
                binding.progressBar.setVisibility(View.GONE);
                binding.btnHint.setEnabled(true);
                if (hint.getHint() != null) hint1 = hint.getHint();
                if (hint.getHint2() != null) hint2 = hint.getHint2();
                hintsRevealed = hint.getHintsRevealed();
                renderHints();
            }

            @Override
            public void onAuthError() {
                binding.progressBar.setVisibility(View.GONE);
                binding.btnHint.setEnabled(true);
                goToAuth();
            }

            @Override
            public void onError(String message) {
                binding.progressBar.setVisibility(View.GONE);
                binding.btnHint.setEnabled(true);
                Toast.makeText(RinjoraPlayRiddleActivity.this, message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void submitAnswer() {
        String answer = binding.etAnswer.getText().toString().trim();
        if (answer.isEmpty()) {
            Toast.makeText(this, "Type an answer first.", Toast.LENGTH_SHORT).show();
            return;
        }
        binding.progressBar.setVisibility(View.VISIBLE);
        binding.btnAnswer.setEnabled(false);
        repository.submitAnswer(riddleId, answer, hint1, hint2,
                new RinjoraRiddleRepository.Callback<AnswerResponseDto>() {
                    @Override
                    public void onSuccess(AnswerResponseDto result) {
                        binding.progressBar.setVisibility(View.GONE);
                        binding.btnAnswer.setEnabled(true);
                        showAnswerResult(result);
                    }

                    @Override
                    public void onAuthError() {
                        binding.progressBar.setVisibility(View.GONE);
                        binding.btnAnswer.setEnabled(true);
                        goToAuth();
                    }

                    @Override
                    public void onError(String message) {
                        binding.progressBar.setVisibility(View.GONE);
                        binding.btnAnswer.setEnabled(true);
                        Toast.makeText(RinjoraPlayRiddleActivity.this, message, Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void showAnswerResult(AnswerResponseDto result) {
        binding.resultCard.setVisibility(View.VISIBLE);
        if (result.isCorrect()) {
            solved = true;
            setLocked(true);
            binding.tvResultTitle.setText("Correct!");
            StringBuilder body = new StringBuilder();
            String msg = result.getMessage();
            if (msg != null && !msg.isEmpty()) {
                body.append(msg);
            } else if (result.isRewarded()) {
                body.append("You earned ").append(result.getPoints()).append(" points.");
            }
            if (result.isRewarded() && !result.isCapped()) {
                body.append("\n\n+").append(result.getPoints()).append(" reputation added.");
            } else if (result.isRewarded() && result.isCapped()) {
                body.append("\n\nPoints capped for today.");
            }
            if (!result.getNewAchievements().isEmpty()) {
                body.append("\n\nNew achievement unlocked!");
                for (rw.martinhardware.mymartin.network.dto.AchievementDto a : result.getNewAchievements()) {
                    body.append("\n🏅 ").append(a.getName());
                }
            }
            binding.tvResultBody.setText(body.toString());
        } else {
            binding.tvResultTitle.setText("Not quite");
            binding.tvResultBody.setText(result.getMessage() != null
                    ? result.getMessage()
                    : "Try again — no points this time.");
        }
    }

    private void revealAnswer() {
        binding.progressBar.setVisibility(View.VISIBLE);
        binding.btnReveal.setEnabled(false);
        repository.reveal(riddleId, new RinjoraRiddleRepository.Callback<RevealDto>() {
            @Override
            public void onSuccess(RevealDto reveal) {
                binding.progressBar.setVisibility(View.GONE);
                binding.btnReveal.setEnabled(true);
                binding.resultCard.setVisibility(View.VISIBLE);
                binding.tvResultTitle.setText("Answer (learning mode)");
                binding.tvResultBody.setText("The answer is:\n\n“" + reveal.getAnswer()
                        + "”\n\nNo points awarded in reveal mode.");
            }

            @Override
            public void onAuthError() {
                binding.progressBar.setVisibility(View.GONE);
                binding.btnReveal.setEnabled(true);
                goToAuth();
            }

            @Override
            public void onError(String message) {
                binding.progressBar.setVisibility(View.GONE);
                binding.btnReveal.setEnabled(true);
                Toast.makeText(RinjoraPlayRiddleActivity.this, message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setLocked(boolean locked) {
        binding.etAnswer.setEnabled(!locked);
        binding.btnAnswer.setEnabled(!locked);
        if (locked) {
            binding.etAnswer.setText("");
        }
    }

    private void logout() {
        new rw.martinhardware.mymartin.data.RinjoraAuthRepository(this)
                .logout(new rw.martinhardware.mymartin.data.RinjoraAuthRepository.AuthCallback() {
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
        Intent intent = new Intent(this, RinjoraAuthActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private String cap(String s) {
        if (s == null || s.isEmpty()) return "";
        return s.substring(0, 1).toUpperCase(java.util.Locale.ROOT) + s.substring(1);
    }
}
