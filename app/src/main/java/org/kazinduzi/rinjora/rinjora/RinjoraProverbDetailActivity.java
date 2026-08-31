package org.kazinduzi.rinjora.rinjora;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import org.kazinduzi.rinjora.databinding.ActivityRinjoraProverbDetailBinding;
import org.kazinduzi.rinjora.entities.RinjoraProverbSnapshot;
import org.kazinduzi.rinjora.data.RinjoraProverbRepository;
import org.kazinduzi.rinjora.network.AuthTokenStore;
import org.kazinduzi.rinjora.network.dto.AnswerResponseDto;
import org.kazinduzi.rinjora.network.dto.RevealDto;

/**
 * Heraheza single-proverb play screen (parity plan §2.4): shows the beginning of
 * the proverb and lets the player complete it using the shared lenient
 * {@code AnswerView} (multi-attempt, concede, reveal). The {@code answer} is only
 * ever shown after a correct solve or an explicit reveal — never cached.
 */
public class RinjoraProverbDetailActivity extends AppCompatActivity {

    private ActivityRinjoraProverbDetailBinding binding;
    private RinjoraProverbRepository repository;

    private long proverbId;
    private boolean solved;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRinjoraProverbDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        repository = new RinjoraProverbRepository(this);

        if (!AuthTokenStore.get(this).hasValidToken()) {
            goToAuth();
            return;
        }

        proverbId = getIntent().getLongExtra("proverb_id", -1);
        if (proverbId <= 0) {
            finish();
            return;
        }

        binding.btnBackToList.setOnClickListener(v -> finish());
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

    private boolean hasCache() {
        return repository.getCached(proverbId) != null;
    }

    private void renderCached() {
        RinjoraProverbSnapshot cache = repository.getCached(proverbId);
        if (cache != null) {
            apply(cache.getQuestion(), cache.getCategoryName(), cache.getSource(), cache.isSolved());
        } else {
            binding.tvQuestion.setText("Loading proverb…");
            binding.tvCategory.setText("Heraheza");
            binding.tvSource.setText("");
        }
    }

    private void refresh() {
        binding.progressBar.setVisibility(View.VISIBLE);
        repository.fetchProverb(proverbId,
                new RinjoraProverbRepository.Callback<RinjoraProverbRepository.ProverbBundle>() {
                    @Override
                    public void onSuccess(RinjoraProverbRepository.ProverbBundle bundle) {
                        binding.progressBar.setVisibility(View.GONE);
                        RinjoraProverbSnapshot s = bundle.snapshot;
                        apply(s.getQuestion(), s.getCategoryName(), s.getSource(), s.isSolved());
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
                            binding.tvQuestion.setText("Couldn’t load proverb: " + message);
                        } else {
                            Toast.makeText(RinjoraProverbDetailActivity.this, message, Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    private void apply(String question, String category, String source, boolean solved) {
        binding.tvQuestion.setText(question);
        binding.tvCategory.setText(category != null && !category.isEmpty() ? category : "Heraheza");
        binding.tvSource.setText(source != null ? source : "");
        this.solved = solved;
        binding.answerView.setLocked(solved);
        if (solved) {
            binding.answerView.showMessage("Solved", "You already solved this proverb.");
        }
    }

    private void submitAnswer(String rawAnswer) {
        binding.answerView.setBusy(true);
        repository.submitAnswer(proverbId, rawAnswer,
                new RinjoraProverbRepository.Callback<AnswerResponseDto>() {
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
                        Toast.makeText(RinjoraProverbDetailActivity.this, message, Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void revealAnswer() {
        binding.progressBar.setVisibility(View.VISIBLE);
        repository.reveal(proverbId, new RinjoraProverbRepository.Callback<RevealDto>() {
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
                Toast.makeText(RinjoraProverbDetailActivity.this, message, Toast.LENGTH_SHORT).show();
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
