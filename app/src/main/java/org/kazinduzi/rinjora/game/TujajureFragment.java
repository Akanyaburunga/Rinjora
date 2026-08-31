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
import java.util.List;

import org.kazinduzi.rinjora.R;
import org.kazinduzi.rinjora.databinding.FragmentTujajureBinding;
import org.kazinduzi.rinjora.data.RinjoraJokeRepository;
import org.kazinduzi.rinjora.entities.RinjoraJokeSnapshot;
import org.kazinduzi.rinjora.network.AuthTokenStore;
import org.kazinduzi.rinjora.network.dto.JokeAnswerResponseDto;
import org.kazinduzi.rinjora.network.dto.RevealDto;
import org.kazinduzi.rinjora.rinjora.RinjoraAuthActivity;
import org.kazinduzi.rinjora.rinjora.RinjoraDuelsActivity;
import org.kazinduzi.rinjora.util.KirundiUi;

/**
 * Tujajure — the fun tab. Hosts the multiple-choice joke round directly:
 * setup + 4 server-order options, pick the punchline, then the next one.
 * Keeps the duels entry.
 */
public class TujajureFragment extends Fragment {

    private FragmentTujajureBinding binding;
    private RinjoraJokeRepository repository;

    private long jokeId;
    private final List<MaterialButton> optionButtons = new ArrayList<>();
    private boolean solved;
    private boolean inFlight;

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
        repository = new RinjoraJokeRepository(requireContext());

        binding.btnFirst.setOnClickListener(v -> startGame());
        binding.btnReveal.setOnClickListener(v -> revealAnswer());
        binding.btnNext.setOnClickListener(v -> nextRound());
        binding.btnDuels.setOnClickListener(v ->
                startActivity(new Intent(requireContext(), RinjoraDuelsActivity.class)));
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
        loadRound();
    }

    private void loadRound() {
        inFlight = true;
        binding.tvStatus.setText(R.string.tuja_status_idle);
        repository.getRound(new RinjoraJokeRepository.Callback<RinjoraJokeRepository.JokeBundle>() {
            @Override
            public void onSuccess(RinjoraJokeRepository.JokeBundle bundle) {
                if (binding == null) return;
                inFlight = false;
                binding.tvStatus.setText("");
                apply(bundle.snapshot.getJokeId(), bundle.snapshot.getSetup(),
                        bundle.options, bundle.snapshot.isSolved());
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
                binding.tvStatus.setText(R.string.tuja_status_idle);
                binding.tvSetup.setText(message);
                Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void apply(long id, String setup, List<String> options, boolean alreadySolved) {
        jokeId = id;
        solved = alreadySolved;
        binding.tvSetup.setText(setup != null ? setup : "");
        binding.resultCard.setVisibility(View.GONE);
        binding.btnNext.setVisibility(View.GONE);
        binding.optionContainer.removeAllViews();
        optionButtons.clear();

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
            btn.setEnabled(!solved && !inFlight);
            btn.setOnClickListener(v -> chooseOption(option, btn));
            binding.optionContainer.addView(btn);
            optionButtons.add(btn);
        }

        binding.btnReveal.setEnabled(!solved && !inFlight);
        binding.btnNext.setVisibility(View.GONE);
        binding.btnReveal.setVisibility(solved ? View.GONE : View.VISIBLE);

        if (solved) {
            binding.resultCard.setVisibility(View.VISIBLE);
            binding.tvResultTitle.setText("Yari imaze gukemuka");
            binding.tvResultBody.setText("Wari waravuze iki kajajuro.");
            binding.btnNext.setVisibility(View.VISIBLE);
        }
    }

    private void chooseOption(final String option, final MaterialButton tapped) {
        if (solved || inFlight) {
            return;
        }
        inFlight = true;
        setEnabledAll(false);

        repository.submitAnswer(jokeId, option,
                new RinjoraJokeRepository.Callback<JokeAnswerResponseDto>() {
                    @Override
                    public void onSuccess(JokeAnswerResponseDto result) {
                        if (binding == null) return;
                        inFlight = false;
                        solved = result.isCorrect();
                        if (solved) {
                            tint(tapped, true);
                        } else {
                            tint(tapped, false);
                            highlightCorrect(result.getAnswer());
                        }
                        showGrade(result);
                        binding.btnNext.setVisibility(View.VISIBLE);
                        binding.btnReveal.setVisibility(View.GONE);
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
                        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
                        setEnabledAll(true);
                    }
                });
    }

    private void revealAnswer() {
        if (solved || inFlight) {
            return;
        }
        inFlight = true;
        repository.reveal(jokeId, new RinjoraJokeRepository.Callback<RevealDto>() {
            @Override
            public void onSuccess(RevealDto reveal) {
                if (binding == null) return;
                inFlight = false;
                String punchline = reveal.getAnswer() == null ? "" : reveal.getAnswer();
                binding.resultCard.setVisibility(View.VISIBLE);
                binding.tvResultTitle.setText("Inyishu (ubwiza bwo kwiga)");
                binding.tvResultBody.setText("Inyishu ni:\n\n\u201C" + punchline
                        + "\u201D\n\nNta manota muri ubu buryo.");
                binding.btnNext.setVisibility(View.VISIBLE);
                binding.btnReveal.setVisibility(View.GONE);
                setEnabledAll(false);
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
                Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
                setEnabledAll(true);
            }
        });
    }

    private void nextRound() {
        inFlight = true;
        binding.resultCard.setVisibility(View.GONE);
        binding.btnNext.setVisibility(View.GONE);
        repository.getNext(new RinjoraJokeRepository.Callback<RinjoraJokeRepository.JokeBundle>() {
            @Override
            public void onSuccess(RinjoraJokeRepository.JokeBundle bundle) {
                if (binding == null) return;
                inFlight = false;
                apply(bundle.snapshot.getJokeId(), bundle.snapshot.getSetup(),
                        bundle.options, bundle.snapshot.isSolved());
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
            binding.tvResultTitle.setText(KirundiUi.goodMessage());
            StringBuilder body = new StringBuilder();
            String msg = result.getMessage();
            if (msg != null && !msg.isEmpty()) {
                body.append(msg);
            } else if (result.isRewarded()) {
                body.append("Wironkeye ").append(result.getPoints()).append(" points.");
            }
            if (result.isRewarded() && !result.isCapped()) {
                body.append("\n\n+").append(result.getPoints()).append(" amanota y'izina.");
            } else if (result.isRewarded()) {
                body.append("\n\nVyagereranije iki kino gihe.");
            }
            if (result.getNewAchievements() != null && !result.getNewAchievements().isEmpty()) {
                body.append("\n\nUfise intsinzi !");
            }
            binding.tvResultBody.setText(body.toString());
        } else {
            binding.tvResultTitle.setText("Ntivyabaye");
            binding.tvResultBody.setText(result.getMessage() != null
                    ? result.getMessage() : "Subira ciwe — ribere aho ry'inyarubanda (icyatsi).");
        }
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
        int bgColor = ContextCompat.getColor(requireContext(),
                correct ? R.color.proto_green : R.color.proto_red);
        btn.setBackgroundColor(bgColor);
        btn.setTextColor(ContextCompat.getColor(requireContext(), R.color.proto_ivory));
    }

    private void setEnabledAll(boolean enabled) {
        for (MaterialButton b : optionButtons) {
            b.setEnabled(enabled);
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
