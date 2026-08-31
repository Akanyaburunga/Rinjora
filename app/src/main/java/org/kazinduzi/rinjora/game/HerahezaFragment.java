package org.kazinduzi.rinjora.game;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import org.kazinduzi.rinjora.databinding.FragmentHerahezaBinding;
import org.kazinduzi.rinjora.rinjora.RinjoraProverbsActivity;

/**
 * Heraheza — "complete the missing words". The player completes the missing word(s)
 * of a Kirundi proverb ({@code GET /proverbs}). The home shows the available
 * proverbs and opens them for play.
 */
public class HerahezaFragment extends Fragment {

    private FragmentHerahezaBinding binding;

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
        binding.btnPlay.setOnClickListener(v ->
                startActivity(new Intent(requireContext(), RinjoraProverbsActivity.class)));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
