package org.kazinduzi.rinjora.game;

import android.animation.ObjectAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
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
import org.kazinduzi.rinjora.data.RinjoraAuthRepository;
import org.kazinduzi.rinjora.data.RinjoraRoundRepository;
import org.kazinduzi.rinjora.network.AuthTokenStore;
import org.kazinduzi.rinjora.network.dto.RoundAnswerDto;
import org.kazinduzi.rinjora.network.dto.RoundCompleteDto;
import org.kazinduzi.rinjora.network.dto.RoundDto;
import org.kazinduzi.rinjora.network.dto.RoundItemDto;
import org.kazinduzi.rinjora.network.dto.RoundStartDto;
import org.kazinduzi.rinjora.rinjora.RinjoraAuthActivity;
import org.kazinduzi.rinjora.util.KirundiUi;
import org.kazinduzi.rinjora.util.PendingGameMode;
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

    private static final String TAG = "QuizFragment";
    private static final String ARG_MODE = "mode";

    /** Session grade for a position already settled in this round. Mirrors the
     *  prototype's {@code answers[i]} record: correct flag, feedback message, streak
     *  flair, reveal text and the player's given answer. {@code answer} is the raw
     *  answer payload from the grade (may be empty if the backend omitted it). */
    private static final class Grade {
        final boolean correct;
        final String msg;
        final String flair;
        final String reveal;
        final String given;
        final String answer;

        Grade(boolean correct, String msg, String flair, String reveal, String given, String answer) {
            this.correct = correct;
            this.msg = msg;
            this.flair = flair;
            this.reveal = reveal;
            this.given = given;
            this.answer = answer;
        }
    }

    private FragmentQuizBinding binding;
    private RinjoraRoundRepository repository;
    private RinjoraAuthRepository authRepository;
    private String mode = "sokwe";

    /** Last-known per-mode guest cap (plan §5), shown on the start screen. */
    private int guestRemaining = -1;
    private int guestLimit = 0;

    private RoundDtoState round;
    private RoundItemDto item;
    private int currentPosition;
    private int level = 1;
    private boolean inFlight;
    private boolean inTrying;
    /** True when this round continues a finished one (Replay / level-up, prototype {@code cont}). */
    private boolean continuing;
    /** The raw text just submitted (prototype's {@code a.given}), shown back disabled. */
    private String lastSubmitted = "";

    /** Positions solved/conceded during this session (never persisted). */
    private final Map<Integer, Grade> solved = new HashMap<>();

    /** Minimal live view over the round: id / itemCount / score / streak / level flags. */
    private static final class RoundDtoState {
        long id;
        String mode;
        int itemCount;
        int score;
        int currentStreak;
        int level;
        int nextLevel;
        int index;
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
        authRepository = new RinjoraAuthRepository(requireContext());

        configureMode();
        wireHitTexts();
        wireButtons();
        showStart();
    }

    private void configureMode() {
        boolean hera = "hera".equals(mode);
        int accent;
        int cardSoft;
        int labColor;
        if (hera) {
            accent = R.color.proto_gold;
            cardSoft = R.color.proto_gold_soft;
            labColor = R.color.proto_gold_dark;
        } else {
            accent = R.color.proto_green;
            cardSoft = R.color.proto_green_soft;
            labColor = R.color.proto_terra;
        }
        // Page header mirrors the prototype's home-card naming (T.nSokwe / T.dSokwe …).
        binding.tvTitle.setText(hera ? KirundiUi.N_HERA : KirundiUi.N_SOKWE);
        binding.tvSubtitle.setText(hera ? KirundiUi.D_HERA : KirundiUi.D_SOKWE);
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
        binding.btnStart.setOnClickListener(v -> {
            continuing = false;
            startGame();
        });
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
        // Prototype: btn-quit → accueil, no confirmation dialog.
        binding.btnQuit.setOnClickListener(v -> showStart());
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
        AuthTokenStore store = AuthTokenStore.get(requireContext());
        if (!store.hasValidToken()) {
            // Never a login wall (plan §6 auth guard): silently mint a guest session.
            authRepository.ensureGuest(noAuth());
            return;
        }
        String pending = PendingGameMode.peekMode(requireContext());
        if (pending != null && pending.equals(mode) && !store.isGuest()) {
            // Fresh account after a cap conversion: resume the round that was blocked.
            PendingGameMode.clear(requireContext());
            startGame();
        }
    }

    // ------------------------------------------------------------------
    // Round lifecycle
    // ------------------------------------------------------------------

    private void startGame() {
        if (inFlight) {
            return;
        }
        if (!AuthTokenStore.get(requireContext()).hasValidToken()) {
            // Guests never hit the login wall (plan §5/§6): provision, then start.
            setBusy(true);
            authRepository.ensureGuest(new RinjoraAuthRepository.AuthCallback() {
                @Override
                public void onSuccess() {
                    if (binding == null) return;
                    setBusy(false);
                    beginStart();
                }

                @Override
                public void onError(String message) {
                    if (binding == null) return;
                    setBusy(false);
                    onSoftError(message);
                }
            });
            return;
        }
        beginStart();
    }

    private void beginStart() {
        // Prototype: a fresh launch from the tab resets the level; only the level-up
        // continuation (or Replay = cont) carries the current level over.
        if (!continuing) level = 1;
        continuing = false;
        solved.clear();
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
                // Positions are 0-based: echo item.position verbatim (plan §"Position contract").
                currentPosition = item.getPosition();
                if (start.getGuest() != null) {
                    guestRemaining = start.getGuest().getRemaining();
                    guestLimit = start.getGuest().getLimit();
                }
                showGame();
                applyItem();
            }

            @Override
            public void onAuthError() {
                if (binding == null) return;
                inFlight = false;
                handleAuthLoss();
            }

            @Override
            public void onError(String message) {
                if (binding == null) return;
                inFlight = false;
                setBusy(false);
                onSoftError(message);
            }

            @Override
            public void onRequiresRegistration(String message) {
                if (binding == null) return;
                inFlight = false;
                setBusy(false);
                showCapPrompt(message);
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
        lastSubmitted = text;
        inFlight = true;
        setBusy(true);
        repository.answer(mode, round.id, currentPosition, text, gradeCallback());
    }

    private void skip() {
        if (inFlight || round == null || item == null || solved.containsKey(currentPosition)) {
            return;
        }
        lastSubmitted = "";
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
                handleAuthLoss();
            }

            @Override
            public void onError(String message) {
                if (binding == null) return;
                inFlight = false;
                setBusy(false);
                onSoftError(message);
            }

            @Override
            public void onRequiresRegistration(String message) {
                if (binding == null) return;
                inFlight = false;
                setBusy(false);
                showCapPrompt(message);
            }
        };
    }

    private void applyGrade(RoundAnswerDto result) {
        if (result.getRound() != null) {
            round = from(result.getRound());
        }
        boolean correct = result.isCorrect();
        boolean conceded = result.isConceded();
        // The grade's answer payload — prefer it, else its revealed_answer.
        String ans = result.getAnswer();
        if (ans == null || ans.trim().isEmpty()) {
            ans = result.getRevealedAnswer();
        }
        boolean blank = ans == null || ans.trim().isEmpty();
        if (correct) {
            String flair = round.currentStreak >= 2 ? KirundiUi.STREAK_MSG : "";
            String given = lastSubmitted != null ? lastSubmitted : "";
            solved.put(currentPosition, new Grade(true, KirundiUi.goodMessage(), flair,
                    revealFor(item, ans), given, ans == null ? "" : ans));
            inTrying = false;
            applyItem();
            binding.confetti.play();
            if (blank) refreshReveal();
        } else if (conceded) {
            solved.put(currentPosition, new Grade(false, KirundiUi.CONCEDE_MSG, "",
                    revealFor(item, ans), "", ans == null ? "" : ans));
            inTrying = false;
            applyItem();
            if (blank) refreshReveal();
        } else {
            // Wrong attempt: impa "trying" state, keep the position, user retypes.
            solved.remove(currentPosition);
            inTrying = true;
            applyItem();
            shake(binding.riddleCard);
        }
    }

    /** Backfills the reveal when the grade response omitted the answer: re-fetch the
     *  per-position item state (G-1), which carries {@code revealed_answer} once the
     *  item is answered, and re-render. Silent — the reveal simply stays blank if the
     *  fetch fails. */
    private void refreshReveal() {
        if (round == null || item == null) {
            return;
        }
        repository.item(mode, round.id, currentPosition, new RinjoraRoundRepository.Callback<RoundItemDto>() {
            @Override
            public void onSuccess(RoundItemDto it) {
                if (binding == null) return;
                item = it;
                applyItem();
            }

            @Override
            public void onAuthError() {
                if (binding != null) handleAuthLoss();
            }

            @Override
            public void onError(String message) {
                // Not fatal: the reveal is already rendered (blank answer).
            }

            @Override
            public void onRequiresRegistration(String message) {
                if (binding != null) showCapPrompt(message);
            }
        });
    }

    private void goBack() {
        if (inFlight || round == null || currentPosition <= 0) {
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
                currentPosition = it.getPosition();
                inTrying = false;
                applyItem();
            }

            @Override
            public void onAuthError() {
                if (binding == null) return;
                inFlight = false;
                setBusy(false);
                handleAuthLoss();
            }

            @Override
            public void onError(String message) {
                if (binding == null) return;
                inFlight = false;
                setBusy(false);
                onSoftError(message);
            }

            @Override
            public void onRequiresRegistration(String message) {
                if (binding == null) return;
                inFlight = false;
                setBusy(false);
                showCapPrompt(message);
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
        // Finished when the server round.index (0-based next pending) reaches itemCount,
        // or the local position is already the last (0-based) item.
        boolean finished = round.index >= round.itemCount || currentPosition + 1 >= round.itemCount;
        if (finished) {
            complete();
            return;
        }
        final int ahead = Math.max(round.index, currentPosition + 1);
        inFlight = true;
        setBusy(true);
        repository.item(mode, round.id, ahead, new RinjoraRoundRepository.Callback<RoundItemDto>() {
            @Override
            public void onSuccess(RoundItemDto it) {
                if (binding == null) return;
                inFlight = false;
                setBusy(false);
                item = it;
                currentPosition = it.getPosition();
                inTrying = false;
                applyItem();
            }

            @Override
            public void onAuthError() {
                if (binding == null) return;
                inFlight = false;
                setBusy(false);
                handleAuthLoss();
            }

            @Override
            public void onError(String message) {
                if (binding == null) return;
                inFlight = false;
                setBusy(false);
                onSoftError(message);
            }

            @Override
            public void onRequiresRegistration(String message) {
                if (binding == null) return;
                inFlight = false;
                setBusy(false);
                showCapPrompt(message);
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
                if (round.levelAvailable && round.score >= 8) {
                    showLevelUp();
                } else {
                    showEnd();
                }
            }

            @Override
            public void onAuthError() {
                if (binding == null) return;
                inFlight = false;
                handleAuthLoss();
            }

            @Override
            public void onError(String message) {
                if (binding == null) return;
                inFlight = false;
                setBusy(false);
                onSoftError(message);
            }

            @Override
            public void onRequiresRegistration(String message) {
                if (binding == null) return;
                inFlight = false;
                setBusy(false);
                showCapPrompt(message);
            }
        });
    }

    private void showLevelUp() {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(KirundiUi.LVL_CHEER)
                .setMessage(KirundiUi.LVL_Q)
                .setPositiveButton(KirundiUi.LVL_YES, (d, w) -> {
                    level = round.nextLevel;
                    continuing = true;
                    startGame();
                })
                .setNegativeButton(KirundiUi.LVL_NO, (d, w) -> showEnd())
                .setCancelable(false)
                .show();
    }

    private void replay() {
        solved.clear();
        continuing = true;
        startGame();
    }

    private void showEnd() {
        binding.gameContainer.setVisibility(View.GONE);
        binding.startContainer.setVisibility(View.GONE);
        binding.endContainer.setVisibility(View.VISIBLE);
        int n = round.itemCount > 0 ? round.itemCount : 1;
        binding.tvEndScore.setText(String.format(Locale.getDefault(), "%d / %d", round.score, n));
        binding.tvEndLab.setText(round.mode != null && round.mode.equals("tuja")
                ? KirundiUi.J_SCORE_LAB : KirundiUi.SCORE_LAB);
        // Prototype: performance is purely client-side by score (>=8 top, >=5 mid).
        binding.tvEndPerf.setText(KirundiUi.performance(round.score, round.itemCount));
        setProgress(100);
        if (round.score >= 5) {
            binding.confetti.play();
        }
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
        continuing = false;
        solved.clear();
        round = null;
        item = null;
        currentPosition = 0;
        inTrying = false;
        binding.gameContainer.setVisibility(View.GONE);
        binding.endContainer.setVisibility(View.GONE);
        binding.startContainer.setVisibility(View.VISIBLE);
        refreshGuestRemaining();
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
            // The grade may have omitted the answer payload; a refreshed item
            // (GET …/items/{position}) carries revealed_answer once answered.
            if ((g.answer == null || g.answer.trim().isEmpty()) && item.getRevealedAnswer() != null) {
                reveal = revealFor(item, item.getRevealedAnswer());
            }
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
        setProgress((int) (round.index * 100f / Math.max(1, n)));

        // eyebrow
        binding.tvCount.setText(KirundiUi.motNombre(pos + 1) + " / " + KirundiUi.motNombre(n));
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
        binding.btnNext.setVisibility(answered ? View.VISIBLE : View.GONE);
        binding.btnSkip.setVisibility(inTrying || answered ? View.GONE : View.VISIBLE);
        binding.btnBack.setVisibility(pos > 0 ? View.VISIBLE : View.GONE);

        // input: disabled with the player's given answer when correct, blank when
        // conceded; cleared and refocused while typing (prototype tries()/conceder()).
        binding.etAnswer.setEnabled(!answered);
        if (answered) {
            binding.etAnswer.setText(correct ? (g != null ? g.given : "") : "");
        } else {
            if (!binding.etAnswer.getText().toString().equals("")) {
                binding.etAnswer.setText("");
            }
            binding.etAnswer.requestFocus();
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
        s.mode = dto.getMode();
        s.itemCount = dto.getItemCount();
        s.score = dto.getScore();
        s.currentStreak = dto.getCurrentStreak();
        s.level = dto.getLevel() > 0 ? dto.getLevel() : level;
        s.nextLevel = dto.getNextLevel() > 0 ? dto.getNextLevel() : level + 1;
        s.levelAvailable = dto.isLevelAvailable();
        s.index = dto.getIndex();
        if (s.level != level) {
            level = s.level;
        }
        return s;
    }

    private void onSoftError(String message) {
        Log.e(TAG, message == null || message.isEmpty() ? "Umukino ntukigeze." : message);
        Toast.makeText(requireContext(),
                message == null || message.isEmpty() ? "Umukino ntukigeze." : message,
                Toast.LENGTH_SHORT).show();
    }

    private RinjoraAuthRepository.AuthCallback noAuth() {
        return new RinjoraAuthRepository.AuthCallback() {
            @Override
            public void onSuccess() {
            }

            @Override
            public void onError(String message) {
                onSoftError(message);
            }
        };
    }

    /** Session lost / expired: clear the game UI and silently restore a guest session. */
    private void handleAuthLoss() {
        showStart();
        authRepository.ensureGuest(noAuth());
    }

    /**
     * The per-mode guest cap (403 {@code requires_registration}, plan §5): offer
     * account creation. On "Later" the pending intent is dropped and the user stays
     * on the start screen; on "Kora aka konto" the auth host opens on the register
     * form with {@code guest_uid} attached, and after login the pending mode relaunches.
     */
    private void showCapPrompt(String message) {
        if (binding == null) return;
        AuthTokenStore store = AuthTokenStore.get(requireContext());
        guestRemaining = 0;
        refreshGuestRemaining();
        if (store.isGuest()) {
            PendingGameMode.set(requireContext(), mode, level);
        }
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(KirundiUi.G_CAP_TITLE)
                .setMessage(message == null || message.isEmpty() ? KirundiUi.G_CAP_MSG : message)
                .setPositiveButton(KirundiUi.G_CAP_GO, (d, w) -> {
                    Intent intent = new Intent(requireContext(), RinjoraAuthActivity.class);
                    intent.putExtra(RinjoraAuthActivity.EXTRA_CREATE_ACCOUNT, true);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                })
                .setNeutralButton(KirundiUi.G_CAP_LOGIN, (d, w) -> {
                    Intent intent = new Intent(requireContext(), RinjoraAuthActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                })
                .setNegativeButton(KirundiUi.G_CAP_LATER, (d, w) -> {
                    PendingGameMode.clear(requireContext());
                    showStart();
                })
                .setCancelable(false)
                .show();
    }

    /** Shows the last-known per-mode guest cap line on the start card when relevant. */
    private void refreshGuestRemaining() {
        if (binding == null || binding.tvGuestRemaining == null) return;
        AuthTokenStore store = AuthTokenStore.get(requireContext());
        if (!store.hasValidToken() || !store.isGuest() || guestRemaining < 0) {
            binding.tvGuestRemaining.setVisibility(View.GONE);
        } else {
            binding.tvGuestRemaining.setText(KirundiUi.guestRemaining(guestRemaining, guestLimit));
            binding.tvGuestRemaining.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}