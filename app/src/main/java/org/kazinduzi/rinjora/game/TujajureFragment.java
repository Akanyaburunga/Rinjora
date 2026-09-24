package org.kazinduzi.rinjora.game;

import android.content.Intent;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.kazinduzi.rinjora.R;
import org.kazinduzi.rinjora.databinding.FragmentTujajureBinding;
import org.kazinduzi.rinjora.data.RinjoraRoundRepository;
import org.kazinduzi.rinjora.network.AuthTokenStore;
import org.kazinduzi.rinjora.network.dto.RoundAnswerDto;
import org.kazinduzi.rinjora.network.dto.RoundCompleteDto;
import org.kazinduzi.rinjora.network.dto.RoundDto;
import org.kazinduzi.rinjora.network.dto.RoundItemDto;
import org.kazinduzi.rinjora.network.dto.RoundStartDto;
import org.kazinduzi.rinjora.rinjora.RinjoraAuthActivity;
import org.kazinduzi.rinjora.util.KirundiUi;

/**
 * Tujajure — the fun tab (parity plan §4.4). Jokes are flat by design: no tiers, no
 * level-up. A round of jokes walks the same {@code POST games/tuja/rounds} → item →
 * option answer → feedback → next → complete loop; tapping an option is the single
 * attempt, wrong picks reveal the punchline (feedback {@code CONCEDE_MSG}).
 */
public class TujajureFragment extends Fragment {

    private FragmentTujajureBinding binding;
    private RinjoraRoundRepository repository;

    private Long roundId;
    private int itemCount;
    private int roundIndex;
    private int score;
    private int currentStreak;
    private RoundItemDto item;
    private int currentPosition;
    private boolean inFlight;

    /** Positions settled this session: position → answered correctly. */
    private final Map<Integer, Boolean> settled = new HashMap<>();
    private final List<MaterialButton> optionButtons = new ArrayList<>();
    private List<String> options = new ArrayList<>();
    private String currentAnswer;
    private String currentChoice;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentTujajureBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        repository = new RinjoraRoundRepository(requireContext());

        binding.tvTitle.setText(KirundiUi.N_TUJA);
        binding.tvSubtitle.setText(KirundiUi.D_TUJA);
        binding.tvStartName.setText(KirundiUi.N_TUJA);
        binding.tvStartDesc.setText(KirundiUi.D_TUJA);
        binding.btnStart.setText(KirundiUi.START_TUJA);
        binding.tvLab.setText(KirundiUi.N_TUJA);
        binding.tvThink.setText(KirundiUi.J_THINK);
        binding.btnNext.setText(KirundiUi.NEXT);
        binding.btnQuit.setText(KirundiUi.QUIT);
        binding.tvEndTitle.setText(KirundiUi.END_TITLE);
        binding.tvEndLab.setText(KirundiUi.J_SCORE_LAB);
        binding.btnReplay.setText(KirundiUi.REPLAY);
        binding.btnShare.setText(KirundiUi.SHARE);
        binding.btnHome.setText(KirundiUi.HOME);

        binding.btnStart.setOnClickListener(v -> startGame());
        binding.btnNext.setOnClickListener(v -> next());
        // Prototype: quit → straight home, no confirmation dialog.
        binding.btnQuit.setOnClickListener(v -> showStart());
        binding.btnReplay.setOnClickListener(v -> replay());
        binding.btnShare.setOnClickListener(v -> share());
        binding.btnHome.setOnClickListener(v -> showStart());

        showStart();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (binding == null) {
            return;
        }
        if (!AuthTokenStore.get(requireContext()).hasValidToken()) {
            goToAuth();
        }
    }

    // ------------------------------------------------------------------
    // Round lifecycle
    // ------------------------------------------------------------------

    private void startGame() {
        if (!AuthTokenStore.get(requireContext()).hasValidToken()) {
            goToAuth();
            return;
        }
        settled.clear();
        inFlight = true;
        setBusy(true);
        repository.start("tuja", null, new RinjoraRoundRepository.Callback<RoundStartDto>() {
            @Override
            public void onSuccess(RoundStartDto start) {
                if (binding == null) return;
                inFlight = false;
                setBusy(false);
                if (start.getRound() == null || start.getItem() == null) {
                    onSoftError("Umukino uriko ubivako. Subira igerageze.");
                    return;
                }
                applyRound(start.getRound());
                item = start.getItem();
                // Positions are 0-based: echo item.position verbatim (plan §"Position contract").
                currentPosition = item.getPosition();
                currentAnswer = null;
                currentChoice = null;
                showGame();
                applyItem();
            }

            @Override
            public void onAuthError() {
                if (binding == null) return;
                inFlight = false;
                goToAuth();
            }

            @Override
            public void onError(String message) {
                if (binding == null) return;
                inFlight = false;
                setBusy(false);
                onSoftError(message);
            }
        });
    }

    private void pick(final String option) {
        if (inFlight || roundId == null || item == null || settled.containsKey(currentPosition)) {
            return;
        }
        inFlight = true;
        setBusy(true);
        repository.answerOption("tuja", roundId, currentPosition, option,
                new RinjoraRoundRepository.Callback<RoundAnswerDto>() {
                    @Override
                    public void onSuccess(RoundAnswerDto result) {
                        if (binding == null) return;
                        inFlight = false;
                        setBusy(false);
                        applyGrade(result, option);
                    }

                    @Override
                    public void onAuthError() {
                        if (binding == null) return;
                        inFlight = false;
                        setBusy(false);
                        goToAuth();
                    }

                    @Override
                    public void onError(String message) {
                        if (binding == null) return;
                        inFlight = false;
                        setBusy(false);
                        onSoftError(message);
                    }
                });
    }

    private void applyGrade(RoundAnswerDto result, String chosen) {
        if (result.getRound() != null) {
            applyRound(result.getRound());
        }
        currentChoice = chosen;
        currentAnswer = result.getAnswer();
        if (currentAnswer == null || currentAnswer.isEmpty()) {
            currentAnswer = result.getRevealedAnswer();
        }
        boolean correct = result.isCorrect();
        boolean blank = currentAnswer == null || currentAnswer.isEmpty();
        settled.put(currentPosition, correct);
        renderOptions();
        if (correct) {
            binding.fbCard.setCardBackgroundColor(ContextCompat.getColor(requireContext(),
                    R.color.proto_green_soft));
            binding.tvFmsg.setText(KirundiUi.goodMessage());
            binding.confetti.play();
        } else {
            binding.fbCard.setCardBackgroundColor(ContextCompat.getColor(requireContext(),
                    R.color.proto_red_soft));
            binding.tvFmsg.setText(KirundiUi.CONCEDE_MSG);
        }
        String reveal = currentAnswer == null || currentAnswer.isEmpty()
                ? null : (KirundiUi.ANSWER_INTRO + " : " + currentAnswer);
        binding.tvFans.setText(reveal);
        binding.tvFans.setVisibility(reveal == null ? View.GONE : View.VISIBLE);
        binding.fbCard.setVisibility(View.VISIBLE);
        binding.btnNext.setVisibility(View.VISIBLE);
        if (blank) {
            refreshReveal();
        }
    }

    /** Backfills the reveal when the grade response omitted the answer: re-fetch the
     *  per-position item state (G-1) and re-render. Silent on failure. */
    private void refreshReveal() {
        if (roundId == null || item == null) {
            return;
        }
        repository.item("tuja", roundId, currentPosition, new RinjoraRoundRepository.Callback<RoundItemDto>() {
            @Override
            public void onSuccess(RoundItemDto it) {
                if (binding == null) return;
                item = it;
                if (currentAnswer == null || currentAnswer.isEmpty()) {
                    currentAnswer = it.getRevealedAnswer();
                }
                applyItem();
            }

            @Override
            public void onAuthError() {
                if (binding != null) goToAuth();
            }

            @Override
            public void onError(String message) {
                // Not fatal: the reveal is already rendered (blank answer).
            }
        });
    }

    private void next() {
        if (inFlight || roundId == null || item == null) {
            return;
        }
        if (!settled.containsKey(currentPosition) && !item.isAnswered()) {
            return;
        }
        // Finished when the server round.index (0-based next pending) reaches itemCount,
        // or the local position is already the last (0-based) item.
        boolean finished = roundIndex >= itemCount || currentPosition + 1 >= itemCount;
        if (finished) {
            complete();
            return;
        }
        final int ahead = Math.max(roundIndex, currentPosition + 1);
        inFlight = true;
        setBusy(true);
        repository.item("tuja", roundId, ahead, new RinjoraRoundRepository.Callback<RoundItemDto>() {
            @Override
            public void onSuccess(RoundItemDto it) {
                if (binding == null) return;
                inFlight = false;
                setBusy(false);
                item = it;
                currentPosition = it.getPosition();
                currentAnswer = it.getRevealedAnswer();
                currentChoice = null;
                applyItem();
            }

            @Override
            public void onAuthError() {
                if (binding == null) return;
                inFlight = false;
                setBusy(false);
                goToAuth();
            }

            @Override
            public void onError(String message) {
                if (binding == null) return;
                inFlight = false;
                setBusy(false);
                onSoftError(message);
            }
        });
    }

    private void complete() {
        inFlight = true;
        setBusy(true);
        repository.complete("tuja", roundId, new RinjoraRoundRepository.Callback<RoundCompleteDto>() {
            @Override
            public void onSuccess(RoundCompleteDto result) {
                if (binding == null) return;
                inFlight = false;
                setBusy(false);
                if (result.getRound() != null) {
                    applyRound(result.getRound());
                }
                showEnd();
            }

            @Override
            public void onAuthError() {
                if (binding == null) return;
                inFlight = false;
                goToAuth();
            }

            @Override
            public void onError(String message) {
                if (binding == null) return;
                inFlight = false;
                setBusy(false);
                onSoftError(message);
            }
        });
    }

    // ------------------------------------------------------------------
    // Rendering
    // ------------------------------------------------------------------

    private void applyRound(RoundDto dto) {
        roundId = dto.getId();
        itemCount = dto.getItemCount();
        roundIndex = dto.getIndex();
        score = dto.getScore();
        currentStreak = dto.getCurrentStreak();
    }

    private void showGame() {
        binding.startContainer.setVisibility(View.GONE);
        binding.endContainer.setVisibility(View.GONE);
        binding.gameContainer.setVisibility(View.VISIBLE);
    }

    private void showStart() {
        inFlight = false;
        settled.clear();
        roundId = null;
        item = null;
        currentPosition = 0;
        roundIndex = 0;
        binding.gameContainer.setVisibility(View.GONE);
        binding.endContainer.setVisibility(View.GONE);
        binding.startContainer.setVisibility(View.VISIBLE);
    }

    private void applyItem() {
        if (roundId == null || item == null) {
            return;
        }
        int pos = currentPosition;
        Boolean settledCorrect = settled.get(pos);
        boolean answered = settledCorrect != null || item.isAnswered();

        // topbar
        binding.pillScore.setText("\u2B50 " + score);
        binding.pillFire.setVisibility(currentStreak > 0 ? View.VISIBLE : View.GONE);
        binding.pillFire.setText("\uD83D\uDD25 " + currentStreak);
        setProgress((int) (roundIndex * 100f / Math.max(1, itemCount)));
        binding.tvCount.setText(KirundiUi.motNombre(pos + 1) + " / " + KirundiUi.motNombre(itemCount));

        binding.tvSetup.setText(item.getText());

        options = item.getOptions();
        renderOptions();
        // Prototype: the think prompt only shows while the joke is open.
        binding.tvThink.setVisibility(answered ? View.GONE : View.VISIBLE);

        if (answered) {
            boolean correct = settledCorrect != null ? settledCorrect : item.isAnsweredCorrect();
            currentAnswer = currentAnswer != null ? currentAnswer : item.getRevealedAnswer();
            binding.fbCard.setCardBackgroundColor(ContextCompat.getColor(requireContext(),
                    correct ? R.color.proto_green_soft : R.color.proto_red_soft));
            binding.tvFmsg.setText(correct ? KirundiUi.goodMessage() : KirundiUi.CONCEDE_MSG);
            String reveal = currentAnswer == null || currentAnswer.isEmpty()
                    ? null : (KirundiUi.ANSWER_INTRO + " : " + currentAnswer);
            binding.tvFans.setText(reveal);
            binding.tvFans.setVisibility(reveal == null ? View.GONE : View.VISIBLE);
            binding.fbCard.setVisibility(View.VISIBLE);
            binding.btnNext.setVisibility(View.VISIBLE);
        } else {
            currentAnswer = null;
            currentChoice = null;
            binding.fbCard.setVisibility(View.GONE);
            binding.btnNext.setVisibility(View.GONE);
        }
    }

    private void renderOptions() {
        binding.optContainer.removeAllViews();
        optionButtons.clear();
        boolean answered = settled.containsKey(currentPosition) || item.isAnswered();
        Boolean correct = settled.get(currentPosition);
        boolean answeredCorrect = correct != null ? correct
                : (item.isAnswered() && item.isAnsweredCorrect());

        for (final String option : options) {
            MaterialButton btn = new MaterialButton(requireContext());
            btn.setText(option);
            btn.setAllCaps(false);
            btn.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
            btn.setGravity(Gravity.CENTER);
            btn.setBackground(makeRounded());
            btn.setTextColor(ContextCompat.getColor(requireContext(), R.color.proto_ink));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.topMargin = dp(8);
            btn.setLayoutParams(lp);

            boolean isChoice = option.equals(currentChoice);
            boolean isAnswer = currentAnswer != null && currentAnswer.trim().equalsIgnoreCase(option.trim());
            if (answered) {
                btn.setEnabled(false);
                if (answeredCorrect && isAnswer) {
                    tint(btn, true);
                } else if (!answeredCorrect && isChoice) {
                    tint(btn, false);
                } else if (!answeredCorrect && isAnswer) {
                    tint(btn, true);
                }
            } else {
                btn.setEnabled(!inFlight);
                btn.setOnClickListener(v -> pick(option));
            }
            binding.optContainer.addView(btn);
            optionButtons.add(btn);
        }
    }

    private void tint(MaterialButton btn, boolean correct) {
        btn.setBackgroundColor(ContextCompat.getColor(requireContext(),
                correct ? R.color.proto_green : R.color.proto_red));
        btn.setTextColor(ContextCompat.getColor(requireContext(), R.color.proto_ivory));
    }

    private void showEnd() {
        binding.gameContainer.setVisibility(View.GONE);
        binding.startContainer.setVisibility(View.GONE);
        binding.endContainer.setVisibility(View.VISIBLE);
        int n = itemCount > 0 ? itemCount : 1;
        binding.tvEndScore.setText(String.format(Locale.getDefault(), "%d / %d", score, n));
        binding.tvEndLab.setText(KirundiUi.J_SCORE_LAB);
        // Prototype: performance is purely client-side by score (>=8 top, >=5 mid).
        binding.tvEndPerf.setText(KirundiUi.performance(score, itemCount));
        setProgress(100);
        if (score >= 5) {
            binding.confetti.play();
        }
    }

    private void replay() {
        settled.clear();
        startGame();
    }

    private void share() {
        Intent i = new Intent(Intent.ACTION_SEND);
        i.setType("text/plain");
        int n = itemCount > 0 ? itemCount : 1;
        i.putExtra(Intent.EXTRA_TEXT, KirundiUi.shareText(score, n));
        startActivity(Intent.createChooser(i, null));
    }

    private void setProgress(int pct) {
        binding.progressTrack.post(() -> {
            if (binding == null) return;
            ViewGroup.LayoutParams lp = binding.progressFill.getLayoutParams();
            lp.width = (int) (binding.progressTrack.getWidth() * Math.min(100, Math.max(0, pct)) / 100f);
            binding.progressFill.setLayoutParams(lp);
        });
    }

    private void setBusy(boolean busy) {
        binding.btnNext.setEnabled(!busy);
        binding.btnQuit.setEnabled(!busy);
        for (MaterialButton b : optionButtons) {
            b.setEnabled(!busy);
        }
    }

    private GradientDrawable makeRounded() {
        GradientDrawable d = new GradientDrawable();
        d.setShape(GradientDrawable.RECTANGLE);
        d.setCornerRadius(dp(16));
        d.setColor(ContextCompat.getColor(requireContext(), R.color.proto_ivory_soft));
        d.setStroke(dp(1), ContextCompat.getColor(requireContext(), R.color.proto_sand));
        return d;
    }

    private void onSoftError(String message) {
        Toast.makeText(requireContext(),
                message == null || message.isEmpty() ? "Umukino ntukigeze." : message,
                Toast.LENGTH_SHORT).show();
    }

    private void goToAuth() {
        Intent intent = new Intent(requireContext(), RinjoraAuthActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }

    private int dp(int v) {
        return Math.round(getResources().getDisplayMetrics().density * v);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}