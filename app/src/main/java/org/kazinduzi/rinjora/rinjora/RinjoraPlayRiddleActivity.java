package org.kazinduzi.rinjora.rinjora;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import org.kazinduzi.rinjora.R;
import org.kazinduzi.rinjora.data.RinjoraRiddleRepository;
import org.kazinduzi.rinjora.databinding.ActivityRinjoraPlayRiddleBinding;
import org.kazinduzi.rinjora.entities.RinjoraRiddleSnapshot;
import org.kazinduzi.rinjora.network.AuthTokenStore;
import org.kazinduzi.rinjora.network.dto.AnswerResponseDto;
import org.kazinduzi.rinjora.network.dto.HintDto;
import org.kazinduzi.rinjora.network.dto.RevealDto;
import org.kazinduzi.rinjora.network.dto.ShareDto;

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
        binding.btnFavorite.setOnClickListener(v -> toggleFavorite());
        binding.btnShare.setOnClickListener(v -> shareRiddle());

        binding.answerView.setConcedeHintVisible(true);
        binding.answerView.setListener(new org.kazinduzi.rinjora.view.AnswerView.Listener() {
            @Override
            public void onSubmit(@NonNull String rawAnswer) {
                submitAnswer(rawAnswer);
            }

            @Override
            public void onReveal() {
                revealAnswer();
            }
        });

        renderCached();
        refresh();
    }

    private boolean favorite;

    private void toggleFavorite() {
        final boolean makeFavorite = !favorite;
        binding.btnFavorite.setEnabled(false);
        repository.setFavorite(riddleId, makeFavorite, new RinjoraRiddleRepository.Callback<Void>() {
            @Override
            public void onSuccess(Void unused) {
                binding.btnFavorite.setEnabled(true);
                favorite = makeFavorite;
                binding.btnFavorite.setText(makeFavorite ? "♥ Saved" : "♥ Save");
                Toast.makeText(RinjoraPlayRiddleActivity.this,
                        makeFavorite ? "Added to favorites." : "Removed from favorites.",
                        Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onAuthError() {
                binding.btnFavorite.setEnabled(true);
                goToAuth();
            }

            @Override
            public void onError(String message) {
                binding.btnFavorite.setEnabled(true);
                Toast.makeText(RinjoraPlayRiddleActivity.this, message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void shareRiddle() {
        binding.progressBar.setVisibility(View.VISIBLE);
        repository.shareRiddle(riddleId, new RinjoraRiddleRepository.Callback<ShareDto>() {
            @Override
            public void onSuccess(ShareDto share) {
                binding.progressBar.setVisibility(View.GONE);
                String url = share.getShareUrl();
                if (url == null || url.isEmpty()) {
                    Toast.makeText(RinjoraPlayRiddleActivity.this, "No share link returned.",
                            Toast.LENGTH_SHORT).show();
                    return;
                }
                Intent send = new Intent(Intent.ACTION_SEND);
                send.setType("text/plain");
                send.putExtra(Intent.EXTRA_TEXT, "Riddle on Kazinduzi: " + url);
                startActivity(Intent.createChooser(send, "Share this riddle"));
            }

            @Override
            public void onAuthError() {
                binding.progressBar.setVisibility(View.GONE);
                goToAuth();
            }

            @Override
            public void onError(String message) {
                binding.progressBar.setVisibility(View.GONE);
                Toast.makeText(RinjoraPlayRiddleActivity.this, message, Toast.LENGTH_SHORT).show();
            }
        });
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
            binding.answerView.showMessage("Solved", "You already solved this riddle.");
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

    private void submitAnswer(String rawAnswer) {
        binding.answerView.setBusy(true);
        repository.submitAnswer(riddleId, rawAnswer, hint1, hint2,
                new RinjoraRiddleRepository.Callback<AnswerResponseDto>() {
                    @Override
                    public void onSuccess(AnswerResponseDto result) {
                        binding.answerView.setBusy(false);
                        if (result.isCorrect()) {
                            solved = true;
                            binding.answerView.setLocked(true);
                        }
                        binding.answerView.showResult(result);
                    }

                    @Override
                    public void onAuthError() {
                        binding.answerView.setBusy(false);
                        goToAuth();
                    }

                    @Override
                    public void onError(String message) {
                        binding.answerView.setBusy(false);
                        Toast.makeText(RinjoraPlayRiddleActivity.this, message, Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void revealAnswer() {
        binding.progressBar.setVisibility(View.VISIBLE);
        repository.reveal(riddleId, new RinjoraRiddleRepository.Callback<RevealDto>() {
            @Override
            public void onSuccess(RevealDto reveal) {
                binding.progressBar.setVisibility(View.GONE);
                binding.answerView.showRevealed(reveal.getAnswer() == null ? "" : reveal.getAnswer());
            }

            @Override
            public void onAuthError() {
                binding.progressBar.setVisibility(View.GONE);
                goToAuth();
            }

            @Override
            public void onError(String message) {
                binding.progressBar.setVisibility(View.GONE);
                Toast.makeText(RinjoraPlayRiddleActivity.this, message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setLocked(boolean locked) {
        binding.answerView.setLocked(locked);
    }

    private void logout() {
        new org.kazinduzi.rinjora.data.RinjoraAuthRepository(this)
                .logout(new org.kazinduzi.rinjora.data.RinjoraAuthRepository.AuthCallback() {
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
