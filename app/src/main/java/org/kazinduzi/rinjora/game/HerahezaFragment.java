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
import org.kazinduzi.rinjora.databinding.FragmentHerahezaBinding;
import org.kazinduzi.rinjora.data.RinjoraProverbRepository;
import org.kazinduzi.rinjora.entities.RinjoraProverbSnapshot;
import org.kazinduzi.rinjora.network.AuthTokenStore;
import org.kazinduzi.rinjora.network.dto.AnswerResponseDto;
import org.kazinduzi.rinjora.network.dto.RevealDto;
import org.kazinduzi.rinjora.rinjora.RinjoraAuthActivity;
import org.kazinduzi.rinjora.view.AnswerView;

/**
 * Heraheza — "complete the missing words". Hosts the proverb fill-blank game
 * directly: next proverb ({@code GET /proverbs/next}), complete it via the
 * shared lenient {@link AnswerView}, then the next one.
 */
public class HerahezaFragment extends Fragment {

    private FragmentHerahezaBinding binding;
    private RinjoraProverbRepository repository;

    private long proverbId;
    private boolean solved;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentHerahezaBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        repository = new RinjoraProverbRepository(requireContext());

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
        binding.btnNext.setOnClickListener(v -> nextProverb());
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
        nextProverb();
    }

    private void nextProverb() {
        binding.tvStatus.setText(R.string.hera_status_idle);
        binding.answerView.resetForNext();
        binding.answerView.setLocked(false);
        binding.tvQuestion.setText("");
        binding.tvSource.setText("");
        binding.btnNext.setVisibility(View.GONE);

        repository.fetchNext(new RinjoraProverbRepository.Callback<RinjoraProverbRepository.ProverbBundle>() {
            @Override
            public void onSuccess(RinjoraProverbRepository.ProverbBundle bundle) {
                if (binding == null) {
                    return;
                }
                binding.tvStatus.setText("");
                RinjoraProverbSnapshot s = bundle.snapshot;
                proverbId = s.getProverbId();
                solved = s.isSolved();
                apply(s.getQuestion(), s.getCategoryName(), s.getSource(), s.isSolved());
            }

            @Override
            public void onAuthError() {
                if (binding == null) return;
                goToAuth();
            }

            @Override
            public void onError(String message) {
                if (binding == null) return;
                binding.tvStatus.setText(R.string.hera_none);
                binding.tvQuestion.setText(message);
                Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void apply(String question, String category, String source, boolean solved) {
        binding.tvQuestion.setText(question != null ? question : "");
        binding.tvCategory.setText(category != null && !category.isEmpty() ? category : "");
        binding.tvSource.setText(source != null ? source : "");
        this.solved = solved;
        binding.answerView.setLocked(solved);
        if (solved) {
            binding.answerView.showMessage("Yari imaze gukemuka", "Wari waravuze uyu mwibutsa.");
            binding.btnNext.setVisibility(View.VISIBLE);
        }
    }

    private void submitAnswer(String rawAnswer) {
        binding.answerView.setBusy(true);
        repository.submitAnswer(proverbId, rawAnswer,
                new RinjoraProverbRepository.Callback<AnswerResponseDto>() {
                    @Override
                    public void onSuccess(AnswerResponseDto result) {
                        if (binding == null) return;
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
        repository.reveal(proverbId, new RinjoraProverbRepository.Callback<RevealDto>() {
            @Override
            public void onSuccess(RevealDto reveal) {
                if (binding == null) return;
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

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
