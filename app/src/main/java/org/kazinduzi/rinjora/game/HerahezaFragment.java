package org.kazinduzi.rinjora.game;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import org.kazinduzi.rinjora.databinding.FragmentHerahezaBinding;

/**
 * Heraheza — "complete the missing words". A Kirundi short expression is shown with
 * one or more gaps and the player fills them in. New game mode; the play screen is
 * implemented in a later phase. Placeholder messaging is shown here.
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
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
