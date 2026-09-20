package org.kazinduzi.rinjora.game;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import org.kazinduzi.rinjora.R;
import org.kazinduzi.rinjora.databinding.FragmentQuizBinding;
import org.kazinduzi.rinjora.data.RinjoraRoundRepository;
import org.kazinduzi.rinjora.network.AuthTokenStore;
import org.kazinduzi.rinjora.network.dto.RoundAnswerDto;
import org.kazinduzi.rinjora.network.dto.RoundCompleteDto;
import org.kazinduzi.rinjora.network.dto.RoundDto;
import org.kazinduzi.rinjora.network.dto.RoundItemDto;
import org.kazinduzi.rinjora.network.dto.RoundStartDto;
import org.kazinduzi.rinjora.rinjora.RinjoraAuthActivity;
import org.kazinduzi.rinjora.util.KirundiUi;
import org.kazinduzi.rinjora.util.TextUtil;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Shared round-of-N quiz for Sokwe and Heraheza (parity plan §4.2). One code path,
 * driven by the {@code mode} nav argument ("sokwe" | "hera"). Server-owned round
 * state: start → item → answer/skip → feedback/confetti → next → complete →
 * end (replay/share/home) with the level-up dialog between complete and end.
 */
public class QuizFragment extends Fragment {

    private static final String ARG_MODE = "mode";
    private static final String PREFS = "rinjora_levels";

    /** Session grade for a position already answered flexibly in-memory. */
    private static final class Grade {
        final boolean correct;
        final String msg;
        final String flair;
        final String reveal;

        Grade(boolean correct, String msg, String flair, String reveal) {
            this.correct = correct;
            this.msg = msg;
            this.flair = flair;
            this.reveal = reveal;
        }
    }

    private FragmentQuizBinding binding;
    private RinjoraRoundRepository repository;
    private String mode = "sokwe";

    private RoundDtoState round;
    private RoundItemDto item;
    private int currentPosition;
    private int level = 1;
    private boolean inFlight;
    private boolean inTrying;
    private boolean ended;

    /** Positions solved/conceded during this session (never persisted). */
    private final Map<Integer, Grade> solved = new HashMap<>();

    /** Minimal live view over the round: id / itemCount / score / streak / level flags. */
    private static final class RoundDtoState {
        long id;
        int itemCount;
        int score;
        int currentStreak;
        int level;
        int nextLevel;
        boolean levelAvailable;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentQuizBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        String m = getArguments() != null ? getArguments().getString(ARG_MODE) : null;
        if (m != null) {
            mode = m;
        }
        repository = new RinjoraRoundRepository(requireContext());
        level = storedLevel();

        configureMode();
        wireHitTexts();
        wireButtons();
        showStart();
    }

    private void configureMode() {
        boolean hera = "hera".equals(mode);
        int title;
        int subtitle;
        int accent;
        int cardSoft;
        int labColor;
        if (hera) {
            title = R.string.hera_title;
            subtitle = R.string.hera_subtitle;
            accent = R.color.proto_gold;
            cardSoft = R.color.proto_gold_soft;
            labColor = R.color.proto_gold_dark;
        } else {
            title = R.string.sokwe_title;
            subtitle = R.string.sokwe_subtitle;
            accent = R.color.proto_green;
            cardSoft = R.color.proto_green_soft;
            labColor = R.color.proto_terra;
        }
        binding.tvTitle.setText(title);
        binding.tvSubtitle.setText(subtitle);
        binding.btnStart.setBackgroundTintList(
                android.content.res.ColorStateList.valueOf(ContextCompat.getColor(requireContext(), accent)));
        binding.riddleCard.setCardBackgroundColor(
                ContextCompat.getColor(requireContext(), cardSoft));
        binding.tvLab.setTextColor(ContextCompat.getColor(requireContext(), labColor));
        binding.tvStartName.setText(hera ? KirundiUi.N_HERA : KirundiUi.N_SOKWE);
        binding.tvStartDesc.setText(hera ? KirundiUi.D_HERA : KirundiUi.D_SOKWE);
        binding.tvLab.setText(hera ? KirundiUi.LAB_HERA : KirundiUi.LAB_SOKWE);
        binding.btnStart.setText(hera ? KirundiUi.START_HERA : KirundiUi.START_SOKWE);
    }

    private void wireHitTexts() {
        boolean hera = "hera".equals(mode);
        binding.etAnswer.setHint(hera ? KirundiUi.PH_HERA : KirundiUi.PH_SOKWE);
        binding.btnCheck.setText(KirundiUi.CHECK);
        binding.btnGive.setText(KirundiUi.GIVE);
        binding.btnSkip.setText(KirundiUi.SKIP);
        binding.btnBack.setText(KirundiUi.BACK);
        binding.btnNext.setText(KirundiUi.NEXT);
        binding.btnQuit.setText(KirundiUi.QUIT);
        binding.tvEndTitle.setText(KirundiUi.END_TITLE);
        binding.tvEndLab.setText(KirundiUi.SCORE_LAB);
        binding.btnReplay.setText(KirundiUi.REPLAY);
        binding.btnShare.setText(KirundiUi.SHARE);
        binding.btnHome.setText(KirundiUi.HOME);
    }

    private void wireButtons() {
        binding.btnStart.setOnClickListener(v -> startGame());
        binding.btnCheck.setOnClickListener(v -> check());
        binding.etAnswer.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE && !binding.etAnswer.isEnabled()) {
                return true;
            }
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                check();
                return true;
            }
            return false;
        });
        binding.btnGive.setOnClickListener(v -> skip());
        binding.btnSkip.setOnClickListener(v -> skip());
        binding.btnNext.setOnClickListener(v -> next());
        binding.btnBack.setOnClickListener(v -> goBack());
        binding.btnQuit.setOnClickListener(v -> confirmQuit());
        binding.btnReplay.setOnClickListener(v -> replay());
        binding.btnShare.setOnClickListener(v -> share());
        binding.btnHome.setOnClickListener(v -> showStart());
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
        solved.clear();
        ended = false;
        inFlight = true;
        setBusy(true);
        repository.start(mode, level, new RinjoraRoundRepository.Callback<RoundStartDto>() {
            @Override
            public void onSuccess(RoundStartDto start) {
                if (binding == null) return;
                inFlight = false;
                setBusy(false);
                if (start.getRound() == null || start.getItem() == null) {
                    onSoftError("Umukino uriko ubivako. Subira igerageze.");
                    return;
                }
                round = from(start.getRound());
                item = start.getItem();
                currentPosition = Math.max(1, item.getPosition());
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

    private void check() {
        if (inFlight || round == null || item == null || solved.containsKey(currentPosition)) {
            return;
        }
        String text = binding.etAnswer.getText() == null ? "" : binding.etAnswer.getText().toString().trim();
        if (text.isEmpty()) {
            Toast.makeText(requireContext(), "Andika inyishu mbere.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (TextUtil.isConcede(text)) {
            skip();
            return;
        }
        inFlight = true;
        setBusy(true);
        repository.answer(mode, round.id, currentPosition, text, gradeCallback());
    }

    private void skip() {
        if (inFlight || round == null || item == null || solved.containsKey(currentPosition)) {
            return;
        }
        inFlight = true;
        setBusy(true);
        repository.skip(mode, round.id, currentPosition, gradeCallback());
    }

    private RinjoraRoundRepository.Callback<RoundAnswerDto> gradeCallback() {
        return new RinjoraRoundRepository.Callback<RoundAnswerDto>() {
            @Override
            public void onSuccess(RoundAnswerDto result) {
                if (binding == null) return;
                inFlight = false;
                setBusy(false);
                applyGrade(result);
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
        };
    }

    private void applyGrade(RoundAnswerDto result) {
        if (result.getRound() != null) {
            round = from(result.getRound());
        }
        boolean correct = result.isCorrect();
        boolean conceded = result.isConceded();
        if (correct) {
            String flair = round.currentStreak >= 2 ? KirundiUi.STREAK_MSG : "";
            solved.put(currentPosition, new Grade(true, KirundiUi.goodMessage(), flair,
                    revealFor(item, result.getAnswer())));
            inTrying = false;
            applyItem();
            binding.confetti.play();
        } else if (conceded) {
            solved.put(currentPosition, new Grade(false, KirundiUi.CONCEDE_MSG, "",
                    revealFor(item, result.getAnswer())));
            inTrying = false;
            applyItem();
        } else {
            // Wrong attempt: impa "trying" state, keep the position, user retypes.
            solved.remove(currentPosition);
            inTrying = true;
            applyItem();
            shake(binding.riddleCard);
        }
    }

    private void goBack() {
        if (inFlight || round == null || currentPosition <= 1) {
            return;
        }
        final int prev = currentPosition - 1;
        inFlight = true;
        setBusy(true);
        repository.item(mode, round.id, prev, new RinjoraRoundRepository.Callback<RoundItemDto>() {
            @Override
            public void onSuccess(RoundItemDto it) {
                if (binding == null) return;
                inFlight = false;
                setBusy(false);
                item = it;
                currentPosition = Math.max(1, it.getPosition());
                inTrying = false;
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

    private void next() {
        if (inFlight || round == null || item == null) {
            return;
        }
        if (!solved.containsKey(currentPosition) && !item.isAnswered()) {
            return;
        }
        if (currentPosition >= round.itemCount) {
            complete();
            return;
        }
        final int ahead = currentPosition + 1;
        inFlight = true;
        setBusy(true);
        repository.item(mode, round.id, ahead, new RinjoraRoundRepository.Callback<RoundItemDto>() {
            @Override
            public void onSuccess(RoundItemDto it) {
                if (binding == null) return;
                inFlight = false;
                setBusy(false);
                item = it;
                currentPosition = Math.max(1, it.getPosition());
                inTrying = false;
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
        repository.complete(mode, round.id, new RinjoraRoundRepository.Callback<RoundCompleteDto>() {
            @Override
            public void onSuccess(RoundCompleteDto result) {
                if (binding == null) return;
                inFlight = false;
                setBusy(false);
                if (result.getRound() != null) {
                    round = from(result.getRound());
                }
                ended = true;
                String perf = result.getPerformance();
                if (round.levelAvailable && round.score >= 8) {
                    showLevelUp(perf);
                } else {
                    showEnd(perf);
                }
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

    private void showLevelUp(final String perf) {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(KirundiUi.LVL_CHEER)
                .setMessage(KirundiUi.LVL_Q)
                .setPositiveButton(KirundiUi.LVL_YES, (d, w) -> {
                    level = round.nextLevel;
                    saveLevel();
                    startGame();
                })
                .setNegativeButton(KirundiUi.LVL_NO, (d, w) -> showEnd(perf))
                .setCancelable(false)
                .show();
    }

    private void replay() {
        solved.clear();
        ended = false;
        startGame();
    }

    private void showEnd(String perf) {
        binding.gameContainer.setVisibility(View.GONE);
        binding.startContainer.setVisibility(View.GONE);
        binding.endContainer.setVisibility(View.VISIBLE);
        int n = round.itemCount > 0 ? round.itemCount : 1;
        binding.tvEndScore.setText(String.format(Locale.getDefault(), "%d / %d", round.score, n));
        String label = "tuja".equals(mode) ? KirundiUi.J_SCORE_LAB : KirundiUi.SCORE_LAB;
        binding.tvEndLab.setText(label);
        binding.tvEndPerf.setText(performanceMessage(perf));
        if (round.score >= 5) {
            binding.confetti.play();
        }
    }

    private String performanceMessage(String perf) {
        if ("top".equals(perf)) return KirundiUi.PERF_TOP;
        if ("mid".equals(perf)) return KirundiUi.PERF_MID;
        if ("low".equals(perf)) return KirundiUi.PERF_LOW;
        return KirundiUi.performance(round.score, round.itemCount);
    }

    private void confirmQuit() {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(KirundiUi.QUIT)
                .setMessage(KirundiUi.QUIT_ASK)
                .setPositiveButton(KirundiUi.LVL_YES, (d, w) -> showStart())
                .setNegativeButton(KirundiUi.LVL_NO, null)
                .show();
    }

    private void share() {
        Intent i = new Intent(Intent.ACTION_SEND);
        i.setType("text/plain");
        int n = round.itemCount > 0 ? round.itemCount : 1;
        i.putExtra(Intent.EXTRA_TEXT, KirundiUi.shareText(round.score, n));
        startActivity(Intent.createChooser(i, null));
    }

    // ------------------------------------------------------------------
    // Rendering
    // ------------------------------------------------------------------

    private void showGame() {
        binding.startContainer.setVisibility(View.GONE);
        binding.endContainer.setVisibility(View.GONE);
        binding.gameContainer.setVisibility(View.VISIBLE);
    }

    private void showStart() {
        inFlight = false;
        ended = false;
        solved.clear();
        round = null;
        item = null;
        currentPosition = 0;
        inTrying = false;
        binding.gameContainer.setVisibility(View.GONE);
        binding.endContainer.setVisibility(View.GONE);
        binding.startContainer.setVisibility(View.VISIBLE);
    }

    private void applyItem() {
        if (round == null || item == null) {
            return;
        }
        int pos = currentPosition;
        int n = round.itemCount;

        // Grade resolution: in-session solved map wins; else server-answered state.
        boolean fresh;
        boolean answered;
        boolean correct;
        String msg = "";
        String flair = "";
        String reveal = null;
        Grade g = solved.get(pos);
        if (g != null) {
            fresh = false;
            answered = true;
            correct = g.correct;
            msg = g.msg;
            flair = g.flair;
            reveal = g.reveal;
        } else if (item.isAnswered()) {
            fresh = false;
            answered = true;
            correct = item.isAnsweredCorrect();
            msg = correct ? KirundiUi.goodMessage() : KirundiUi.CONCEDE_MSG;
            reveal = revealFor(item, item.getRevealedAnswer());
        } else if (inTrying) {
            fresh = false;
            answered = false;
            correct = false;
            msg = KirundiUi.IMPA;
        } else {
            fresh = true;
            answered = false;
            correct = false;
        }

        // topbar
        binding.pillScore.setText("\u2B50 " + round.score);
        binding.pillFire.setVisibility(round.currentStreak > 0 ? View.VISIBLE : View.GONE);
        binding.pillFire.setText("\uD83D\uDD25 " + round.currentStreak);
        setProgress((int) ((pos - 1) * 100f / Math.max(1, n)));

        // eyebrow
        binding.tvCount.setText(KirundiUi.motNombre(pos) + " / " + KirundiUi.motNombre(n));
        binding.tvLevel.setText(KirundiUi.LEVEL + " " + round.level);

        binding.tvRiddle.setText(item.getText());

        // feedback card
        binding.fbCard.setCardBackgroundColor(ContextCompat.getColor(requireContext(),
                (fresh || correct) ? R.color.proto_green_soft : R.color.proto_red_soft));
        binding.tvFmsg.setText(msg);
        binding.tvFstreak.setText(flair);
        binding.tvFstreak.setVisibility(flair == null || flair.isEmpty() ? View.GONE : View.VISIBLE);
        binding.tvFans.setText(reveal);
        binding.tvFans.setVisibility(reveal == null || reveal.isEmpty() ? View.GONE : View.VISIBLE);
        binding.fbCard.setVisibility((answered || inTrying) ? View.VISIBLE : View.GONE);

        // primary buttons
        binding.btnCheck.setVisibility(answered ? View.GONE : View.VISIBLE);
        binding.btnGive.setVisibility(inTrying ? View.VISIBLE : View.GONE);

        // ghost buttons
        binding.btnNext.setVisibility(fresh ? View.GONE : View.VISIBLE);
        binding.btnSkip.setVisibility(inTrying || answered ? View.GONE : View.VISIBLE);
        binding.btnBack.setVisibility(pos > 1 ? View.VISIBLE : View.GONE);

        // input
        binding.etAnswer.setEnabled(!answered);
        if (answered) {
            binding.etAnswer.setText("");
        }
    }

    private void setProgress(int pct) {
        binding.progressTrack.post(() -> {
            if (binding == null) return;
            ViewGroup.LayoutParams lp = binding.progressFill.getLayoutParams();
            lp.width = (int) (binding.progressTrack.getWidth() * Math.min(100, Math.max(0, pct)) / 100f);
            binding.progressFill.setLayoutParams(lp);
        });
    }

    private void shake(View target) {
        ObjectAnimator.ofFloat(target, View.TRANSLATION_X,
                0f, -12f, 10f, -8f, 6f, -4f, 0f).setDuration(380).start();
    }

    private void setBusy(boolean busy) {
        binding.btnCheck.setEnabled(!busy);
        binding.btnGive.setEnabled(!busy);
        binding.btnSkip.setEnabled(!busy);
        binding.btnBack.setEnabled(!busy);
        binding.btnNext.setEnabled(!busy);
        binding.btnQuit.setEnabled(!busy);
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private String revealFor(RoundItemDto it, String answer) {
        String ans = answer == null ? "" : answer.trim();
        if (ans.isEmpty() && it != null && it.getRevealedAnswer() != null) {
            ans = it.getRevealedAnswer().trim();
        }
        if ("hera".equals(mode) && it != null) {
            String q = it.getText();
            String stem = q.replaceAll("[…]+\\s*$", "").replaceAll("\\.\\.\\.\\s*$", "").trim();
            String first = ans.isEmpty() ? "" : ans.split("/")[0].trim();
            return KirundiUi.HERA_INTRO + " : " + stem + " " + first;
        }
        return KirundiUi.ANSWER_INTRO + " : " + ans;
    }

    private RoundDtoState from(RoundDto dto) {
        RoundDtoState s = new RoundDtoState();
        s.id = dto.getId();
        s.itemCount = dto.getItemCount();
        s.score = dto.getScore();
        s.currentStreak = dto.getCurrentStreak();
        s.level = dto.getLevel() > 0 ? dto.getLevel() : level;
        s.nextLevel = dto.getNextLevel() > 0 ? dto.getNextLevel() : level + 1;
        s.levelAvailable = dto.isLevelAvailable();
        if (s.level != level) {
            level = s.level;
        }
        return s;
    }

    private void onSoftError(String message) {
        Toast.makeText(requireContext(),
                message == null || message.isEmpty() ? "Umukino ntukigeze." : message,
                Toast.LENGTH_SHORT).show();
    }

    private int storedLevel() {
        SharedPreferences prefs = requireContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        return Math.max(1, prefs.getInt("level_" + mode, 1));
    }

    private void saveLevel() {
        requireContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit().putInt("level_" + mode, level).apply();
    }

    private void goToAuth() {
        Intent intent = new Intent(requireContext(), RinjoraAuthActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}