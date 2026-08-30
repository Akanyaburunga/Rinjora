package org.kazinduzi.rinjora.game;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import org.kazinduzi.rinjora.databinding.FragmentJeweBinding;
import org.kazinduzi.rinjora.data.GuestProgressRepository;
import org.kazinduzi.rinjora.entities.GuestPlayer;

/**
 * Jewe — "me". The personal tab: guest progress, an option to create an account,
 * and (once an account exists) a sync of locally recorded progress to the server.
 */
public class JeweFragment extends Fragment {

    private FragmentJeweBinding binding;
    private GuestProgressRepository repository;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentJeweBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        repository = new GuestProgressRepository(requireContext());
        render();

        binding.btnSync.setOnClickListener(v -> syncPending());
    }

    @Override
    public void onResume() {
        super.onResume();
        if (binding != null) {
            render();
        }
    }

    private void render() {
        GuestPlayer p = repository.getOrCreatePlayer();
        boolean loggedIn = repository.isLoggedIn();

        binding.tvPoints.setText(String.valueOf(p.getTotalPoints()));
        binding.tvSolved.setText(String.valueOf(p.getRiddlesSolved()));
        binding.tvStreak.setText(String.valueOf(p.getCurrentStreak()));

        long pending = repository.countPending();
        if (loggedIn) {
            binding.tvGuestPrompt.setText("You're signed in. Local progress is stored on your account."
                    + (pending > 0 ? " " + pending + " record(s) waiting to sync." : ""));
            binding.btnSync.setVisibility(View.VISIBLE);
        } else {
            binding.tvGuestPrompt.setText("You're playing as a guest. Your progress is saved on this device."
                    + " Create an account later to carry it anywhere.");
            binding.btnSync.setVisibility(View.GONE);
        }
    }

    private void syncPending() {
        repository.syncPending(new GuestProgressRepository.SyncCallback() {
            @Override
            public void onSynced(int uploaded) {
                Toast.makeText(requireContext(),
                        "Synced " + uploaded + " record(s).", Toast.LENGTH_SHORT).show();
                render();
            }

            @Override
            public void onNotAuthenticated() {
                Toast.makeText(requireContext(), "Create an account first to sync progress.",
                        Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onError(String message) {
                Toast.makeText(requireContext(), "Sync failed: " + message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
