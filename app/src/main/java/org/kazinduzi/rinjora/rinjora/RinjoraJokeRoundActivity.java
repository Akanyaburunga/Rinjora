package org.kazinduzi.rinjora.rinjora;

import android.content.Intent;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;

import org.kazinduzi.rinjora.R;
import org.kazinduzi.rinjora.databinding.ActivityRinjoraJokeRoundBinding;
import org.kazinduzi.rinjora.data.RinjoraJokeRepository;
import org.kazinduzi.rinjora.entities.RinjoraJokeSnapshot;
import org.kazinduzi.rinjora.network.AuthTokenStore;
import org.kazinduzi.rinjora.network.dto.JokeAnswerResponseDto;
import org.kazinduzi.rinjora.network.dto.RevealDto;

/**
 * Tujajure joke round (parity plan §3): shows the setup and the 4 server-order options,
 * one tap submits the chosen option. On correct the tapped option turns green and we
 * move on; on wrong the tapped option turns red and the correct one green (from the
 * backend's returned punchline), then next. Options are never re-shuffled client-side and
 * the punchline is never cached on its own — only the 4 indistinguishable options are.
 */
public class RinjoraJokeRoundActivity extends AppCompatActivity {

    private ActivityRinjoraJokeRoundBinding binding;
    private RinjoraJokeRepository repository;

    private long jokeId;
    private final List<MaterialButton> optionButtons = new ArrayList<>();
    private boolean solved;
    private boolean inFlight;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRinjoraJokeRoundBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        repository = new RinjoraJokeRepository(this);

        if (!AuthTokenStore.get(this).hasValidToken()) {
            goToAuth();
            return;
        }

        binding.btnReveal.setOnClickListener(v -> revealAnswer());
        binding.btnNext.setOnClickListener(v -> nextRound());

        renderCached();
        loadRound();
    }

    private boolean hasCache() {
        return repository.getCached() != null;
    }

    private void renderCached() {
        RinjoraJokeSnapshot cache = repository.getCached();
        if (cache != null) {
            apply(cache.getJokeId(), cache.getSetup(), repository.optionsOf(cache), cache.isSolved());
        } else {
            binding.tvSetup.setText("Loading joke…");
        }
    }

    private void loadRound() {
        inFlight = true;
        binding.progressBar.setVisibility(View.VISIBLE);

        repository.getRound(new RinjoraJokeRepository.Callback<RinjoraJokeRepository.JokeBundle>() {
            @Override
            public void onSuccess(RinjoraJokeRepository.JokeBundle bundle) {
                inFlight = false;
                binding.progressBar.setVisibility(View.GONE);
                apply(bundle.snapshot.getJokeId(), bundle.snapshot.getSetup(), bundle.options,
                        bundle.snapshot.isSolved());
            }

            @Override
            public void onAuthError() {
                inFlight = false;
                binding.progressBar.setVisibility(View.GONE);
                goToAuth();
            }

            @Override
            public void onError(String message) {
                inFlight = false;
                binding.progressBar.setVisibility(View.GONE);
                if (!hasCache()) {
                    binding.tvSetup.setText("Couldn’t load a joke: " + message);
                } else {
                    Toast.makeText(RinjoraJokeRoundActivity.this, message, Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void apply(long id, String setup, List<String> options, boolean alreadySolved) {
        jokeId = id;
        solved = alreadySolved;
        binding.tvSetup.setText(setup != null ? setup : "");
        binding.progressBar.setVisibility(View.GONE);
        binding.resultCard.setVisibility(View.GONE);
        binding.btnNext.setVisibility(View.GONE);
        binding.optionContainer.removeAllViews();
        optionButtons.clear();

        for (final String option : options) {
            MaterialButton btn = new MaterialButton(this);
            btn.setText(option);
            btn.setAllCaps(false);
            btn.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
            btn.setGravity(android.view.Gravity.CENTER);
            btn.setBackground(makeRounded(R.color.brand_surface));
            btn.setTextColor(ContextCompat.getColor(this, R.color.text_primary));
            android.widget.LinearLayout.LayoutParams lp = new android.widget.LinearLayout.LayoutParams(
                    android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                    android.widget.LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.topMargin = dp(8);
            btn.setLayoutParams(lp);
            btn.setEnabled(!solved && !inFlight);
            btn.setOnClickListener(v -> chooseOption(option, btn));
            binding.optionContainer.addView(btn);
            optionButtons.add(btn);
        }

        binding.btnReveal.setEnabled(!solved && !inFlight);
        binding.btnNext.setVisibility(View.GONE);

        if (solved) {
            binding.resultCard.setVisibility(View.VISIBLE);
            binding.tvResultTitle.setText("Solved");
            binding.tvResultBody.setText("You already solved this joke.");
            binding.btnNext.setVisibility(View.VISIBLE);
        }
    }

    private void chooseOption(final String option, final MaterialButton tapped) {
        if (solved || inFlight) {
            return;
        }
        inFlight = true;
        setEnabledAll(false);
        binding.progressBar.setVisibility(View.VISIBLE);

        repository.submitAnswer(jokeId, option,
                new RinjoraJokeRepository.Callback<JokeAnswerResponseDto>() {
                    @Override
                    public void onSuccess(JokeAnswerResponseDto result) {
                        inFlight = false;
                        binding.progressBar.setVisibility(View.GONE);
                        solved = result.isCorrect();
                        if (solved) {
                            tint(tapped, true);
                        } else {
                            tint(tapped, false);
                            highlightCorrect(result.getAnswer());
                        }
                        showGrade(result);
                        binding.btnNext.setVisibility(View.VISIBLE);
                    }

                    @Override
                    public void onAuthError() {
                        inFlight = false;
                        binding.progressBar.setVisibility(View.GONE);
                        goToAuth();
                    }

                    @Override
                    public void onError(String message) {
                        inFlight = false;
                        binding.progressBar.setVisibility(View.GONE);
                        Toast.makeText(RinjoraJokeRoundActivity.this, message, Toast.LENGTH_SHORT).show();
                        setEnabledAll(true);
                    }
                });
    }

    private void revealAnswer() {
        if (solved || inFlight) {
            return;
        }
        inFlight = true;
        binding.progressBar.setVisibility(View.VISIBLE);

        repository.reveal(jokeId, new RinjoraJokeRepository.Callback<RevealDto>() {
            @Override
            public void onSuccess(RevealDto reveal) {
                inFlight = false;
                binding.progressBar.setVisibility(View.GONE);
                String punchline = reveal.getAnswer() == null ? "" : reveal.getAnswer();
                showRevealed(punchline);
                binding.btnNext.setVisibility(View.VISIBLE);
            }

            @Override
            public void onAuthError() {
                inFlight = false;
                binding.progressBar.setVisibility(View.GONE);
                goToAuth();
            }

            @Override
            public void onError(String message) {
                inFlight = false;
                binding.progressBar.setVisibility(View.GONE);
                Toast.makeText(RinjoraJokeRoundActivity.this, message, Toast.LENGTH_SHORT).show();
                setEnabledAll(true);
            }
        });
    }

    private void nextRound() {
        inFlight = true;
        binding.resultCard.setVisibility(View.GONE);
        binding.btnNext.setVisibility(View.GONE);
        binding.progressBar.setVisibility(View.VISIBLE);

        repository.getNext(new RinjoraJokeRepository.Callback<RinjoraJokeRepository.JokeBundle>() {
            @Override
            public void onSuccess(RinjoraJokeRepository.JokeBundle bundle) {
                inFlight = false;
                binding.progressBar.setVisibility(View.GONE);
                apply(bundle.snapshot.getJokeId(), bundle.snapshot.getSetup(), bundle.options,
                        bundle.snapshot.isSolved());
            }

            @Override
            public void onAuthError() {
                inFlight = false;
                binding.progressBar.setVisibility(View.GONE);
                goToAuth();
            }

            @Override
            public void onError(String message) {
                inFlight = false;
                binding.progressBar.setVisibility(View.GONE);
                binding.tvSetup.setText(message);
                binding.optionContainer.removeAllViews();
                optionButtons.clear();
                binding.btnNext.setVisibility(View.GONE);
            }
        });
    }

    private void showGrade(JokeAnswerResponseDto result) {
        binding.resultCard.setVisibility(View.VISIBLE);
        if (result.isCorrect()) {
            binding.tvResultTitle.setText("Correct! 🎉");
            StringBuilder body = new StringBuilder();
            String msg = result.getMessage();
            if (msg != null && !msg.isEmpty()) {
                body.append(msg);
            } else if (result.isRewarded()) {
                body.append("You earned ").append(result.getPoints()).append(" points.");
            }
            if (result.isRewarded() && !result.isCapped()) {
                body.append("\n\n+").append(result.getPoints()).append(" reputation added.");
            } else if (result.isRewarded()) {
                body.append("\n\nPoints capped for today.");
            }
            if (result.getNewAchievements() != null && !result.getNewAchievements().isEmpty()) {
                body.append("\n\nNew achievement unlocked!");
            }
            binding.tvResultBody.setText(body.toString());
        } else {
            binding.tvResultTitle.setText("Not quite");
            binding.tvResultBody.setText(result.getMessage() != null
                    ? result.getMessage() : "Keep going — tap the green one for the pun.");
        }
    }

    private void showRevealed(String punchline) {
        binding.resultCard.setVisibility(View.VISIBLE);
        binding.tvResultTitle.setText("Punchline (learning mode)");
        binding.tvResultBody.setText("The punchline is:\n\n“" + punchline
                + "”\n\nNo points awarded in reveal mode.");
        setEnabledAll(false);
    }

    private void highlightCorrect(String correctPunchline) {
        if (correctPunchline == null || correctPunchline.isEmpty()) {
            return;
        }
        for (MaterialButton b : optionButtons) {
            if (correctPunchline.trim().equalsIgnoreCase(b.getText().toString().trim())) {
                tint(b, true);
                break;
            }
        }
    }

    private void tint(MaterialButton btn, boolean correct) {
        int bgColor = ContextCompat.getColor(this,
                correct ? R.color.brand_success : R.color.brand_error);
        btn.setBackgroundColor(bgColor);
        btn.setTextColor(ContextCompat.getColor(this, R.color.white));
    }

    private void setEnabledAll(boolean enabled) {
        for (MaterialButton b : optionButtons) {
            b.setEnabled(enabled);
        }
        binding.btnReveal.setEnabled(enabled);
    }

    private GradientDrawable makeRounded(int colorRes) {
        GradientDrawable d = new GradientDrawable();
        d.setShape(GradientDrawable.RECTANGLE);
        d.setCornerRadius(dp(16));
        d.setColor(ContextCompat.getColor(this, colorRes));
        d.setStroke(dp(1), ContextCompat.getColor(this, R.color.brand_outline));
        return d;
    }

    private void goToAuth() {
        Intent intent = new Intent(this, RinjoraAuthActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private int dp(int v) {
        return Math.round(getResources().getDisplayMetrics().density * v);
    }
}
