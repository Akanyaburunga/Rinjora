package org.kazinduzi.rinjora.game;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import org.kazinduzi.rinjora.databinding.FragmentTujajureBinding;
import org.kazinduzi.rinjora.rinjora.RinjoraDuelsActivity;
import org.kazinduzi.rinjora.rinjora.RinjoraJokeRoundActivity;

/**
 * Tujajure — "let's enjoy some good joking". The fun/social side of the app: jokes
 * (pick the punchline from four options) and duels between friends.
 */
public class TujajureFragment extends Fragment {

    private FragmentTujajureBinding binding;

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
        binding.btnJokes.setOnClickListener(v ->
                startActivity(new Intent(requireContext(), RinjoraJokeRoundActivity.class)));
        binding.btnDuels.setOnClickListener(v ->
                startActivity(new Intent(requireContext(), RinjoraDuelsActivity.class)));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
