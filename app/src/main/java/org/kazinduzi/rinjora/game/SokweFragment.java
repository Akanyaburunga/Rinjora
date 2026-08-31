package org.kazinduzi.rinjora.game;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import org.kazinduzi.rinjora.R;
import org.kazinduzi.rinjora.databinding.FragmentSokweBinding;
import org.kazinduzi.rinjora.data.RinjoraRiddleRepository;
import org.kazinduzi.rinjora.entities.RinjoraRiddleSnapshot;
import org.kazinduzi.rinjora.network.AuthTokenStore;
import org.kazinduzi.rinjora.network.dto.AnswerResponseDto;
import org.kazinduzi.rinjora.network.dto.HintDto;
import org.kazinduzi.rinjora.network.dto.RevealDto;
import org.kazinduzi.rinjora.rinjora.RinjoraAuthActivity;
import org.kazinduzi.rinjora.rinjora.RinjoraDailyActivity;
import org.kazinduzi.rinjora.view.AnswerView;

import java.util.Locale;

/**
 * Sokwe — the wise rabbit's tab. Now hosts the riddle game directly: next riddle
 * ({@code GET /riddles/next}), progressive hints, the shared lenient
 * {@link AnswerView}, and a next-question loop. The daily riddle remains as a
 * quick entry.
 */
public class SokweFragment extends Fragment {

    private FragmentSokweBinding binding;
    private RinjoraRiddleRepository repository;

    private long riddleId;
    private int hintsRevealed;
    private String hint1;
    private String hint2;
    private boolean solved;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentSokweBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        repository = new RinjoraRiddleRepository(requireContext());

        binding.answerView.setConcedeHintVisible(true);
        binding.answerView.setListener(new AnswerView.Listener() {
            @Override
            public void onSubmit(@NonNull String rawAnswer) {
                submitAnswer(rawAnswer);
            }

            @Override
            public void onReveal() {
                revealAnswer();
            }
        });

        binding.btnFirst.setOnClickListener(v -> startGame());
        binding.btnHint.setOnClickListener(v -> revealHint());
        binding.btnNext.setOnClickListener(v -> nextRiddle());
        binding.btnDaily.setOnClickListener(v ->
                startActivity(new Intent(requireContext(), RinjoraDailyActivity.class)));
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

    private void startGame() {
        if (!AuthTokenStore.get(requireContext()).hasValidToken()) {
            goToAuth();
            return;
        }
        binding.btnFirst.setVisibility(View.GONE);
        binding.gameContainer.setVisibility(View.VISIBLE);
        nextRiddle();
    }

    private void nextRiddle() {
        binding.tvStatus.setText(R.string.sokwe_loading);
        binding.answerView.resetForNext();
        binding.answerView.setLocked(false);
        binding.tvQuestion.setText("");
        binding.tvHint1.setVisibility(View.GONE);
        binding.tvHint2.setVisibility(View.GONE);
        binding.tvHintsLabel.setVisibility(View.GONE);
        binding.btnHint.setVisibility(View.GONE);
        binding.btnNext.setVisibility(View.GONE);
        hint1 = null;
        hint2 = null;

        repository.fetchNext(new RinjoraRiddleRepository.Callback<RinjoraRiddleRepository.RiddleBundle>() {
            @Override
            public void onSuccess(RinjoraRiddleRepository.RiddleBundle bundle) {
                if (binding == null) {
                    return;
                }
                binding.tvStatus.setText("");
                RinjoraRiddleSnapshot s = bundle.snapshot;
                riddleId = s.getRiddleId();
                solved = s.isSolved();
                apply(s.getQuestion(), s.getDifficulty(), s.getRiddleType(),
                        s.getCategoryName(), s.getHintsRevealed(), s.isSolved(),
                        bundle.hint1, bundle.hint2);
            }

            @Override
            public void onAuthError() {
                if (binding == null) {
                    return;
                }
                goToAuth();
            }

            @Override
            public void onError(String message) {
                if (binding == null) {
                    return;
                }
                binding.tvStatus.setText(R.string.sokwe_none);
                binding.tvQuestion.setText(message);
                Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void apply(String question, String difficulty, String type, String category,
                       int hintsRevealed, boolean solved, String h1, String h2) {
        binding.tvQuestion.setText(question != null ? question : "");

        StringBuilder meta = new StringBuilder();
        if (difficulty != null && !difficulty.isEmpty()) {
            meta.append(cap(difficulty));
        }
        if (type != null && !type.isEmpty()) {
            if (meta.length() > 0) meta.append(" · ");
            meta.append(cap(type.replace("_", " ")));
        }
        binding.tvMeta.setText(meta.toString());
        binding.tvCategory.setText(category != null ? category : "");

        if (h1 != null) hint1 = h1;
        if (h2 != null) hint2 = h2;
        this.hintsRevealed = hintsRevealed;
        this.solved = solved;

        renderHints();
        binding.answerView.setLocked(solved);

        binding.tvStatus.setText(String.format(Locale.getDefault(),
                "%s %d", binding.tvCategory.getText(), riddleId > 0 ? riddleId : 0));
        if (solved) {
            binding.answerView.showMessage("Yari imaze gukemuka", "Wari waravuze iki gisokozo.");
        }
    }

    private void renderHints() {
        if (hint1 == null) {
            binding.tvHintsLabel.setVisibility(View.GONE);
            binding.tvHint1.setVisibility(View.GONE);
            binding.tvHint2.setVisibility(View.GONE);
            binding.btnHint.setVisibility(View.GONE);
            return;
        }
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

    private void revealHint() {
        repository.requestHint(riddleId, new RinjoraRiddleRepository.Callback<HintDto>() {
            @Override
            public void onSuccess(HintDto hint) {
                if (binding == null) {
                    return;
                }
                if (hint.getHint() != null) hint1 = hint.getHint();
                if (hint.getHint2() != null) hint2 = hint.getHint2();
                hintsRevealed = hint.getHintsRevealed();
                renderHints();
            }

            @Override
            public void onAuthError() {
                if (binding == null) return;
                goToAuth();
            }

            @Override
            public void onError(String message) {
                if (binding == null) return;
                Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void submitAnswer(String rawAnswer) {
        binding.answerView.setBusy(true);
        repository.submitAnswer(riddleId, rawAnswer, hint1, hint2,
                new RinjoraRiddleRepository.Callback<AnswerResponseDto>() {
                    @Override
                    public void onSuccess(AnswerResponseDto result) {
                        if (binding == null) {
                            return;
                        }
                        binding.answerView.setBusy(false);
                        if (result.isCorrect()) {
                            solved = true;
                            binding.answerView.setLocked(true);
                        }
                        binding.answerView.showResult(result);
                        binding.btnNext.setVisibility(View.VISIBLE);
                    }

                    @Override
                    public void onAuthError() {
                        if (binding == null) return;
                        binding.answerView.setBusy(false);
                        goToAuth();
                    }

                    @Override
                    public void onError(String message) {
                        if (binding == null) return;
                        binding.answerView.setBusy(false);
                        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void revealAnswer() {
        repository.reveal(riddleId, new RinjoraRiddleRepository.Callback<RevealDto>() {
            @Override
            public void onSuccess(RevealDto reveal) {
                if (binding == null) {
                    return;
                }
                binding.answerView.showRevealed(reveal.getAnswer() == null ? "" : reveal.getAnswer());
                binding.btnNext.setVisibility(View.VISIBLE);
            }

            @Override
            public void onAuthError() {
                if (binding == null) return;
                goToAuth();
            }

            @Override
            public void onError(String message) {
                if (binding == null) return;
                Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void goToAuth() {
        Intent intent = new Intent(requireContext(), RinjoraAuthActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }

    private String cap(String s) {
        if (s == null || s.isEmpty()) return "";
        return s.substring(0, 1).toUpperCase(Locale.ROOT) + s.substring(1);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
