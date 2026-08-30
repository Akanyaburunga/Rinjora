package org.kazinduzi.rinjora.game;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import org.kazinduzi.rinjora.R;
import org.kazinduzi.rinjora.databinding.FragmentSokweBinding;
import org.kazinduzi.rinjora.rinjora.RinjoraDailyActivity;
import org.kazinduzi.rinjora.rinjora.RinjoraPlayActivity;

/**
 * Sokwe ("umurima w'ibisokwe") — the wise-rabbit hub. Cleverness and wisdom live
 * here: the main "play riddles" entry plus the daily riddle. Guests play immediately;
 * progress is tucked into local ObjectBox until they create an account and sync.
 */
public class SokweFragment extends Fragment {

    private FragmentSokweBinding binding;

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

        binding.btnPlay.setOnClickListener(v ->
                startActivity(new Intent(requireContext(), RinjoraPlayActivity.class)));
        binding.btnDaily.setOnClickListener(v ->
                startActivity(new Intent(requireContext(), RinjoraDailyActivity.class)));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
