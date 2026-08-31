package org.kazinduzi.rinjora.game;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import org.kazinduzi.rinjora.R;
import org.kazinduzi.rinjora.databinding.FragmentJeweBinding;
import org.kazinduzi.rinjora.data.GuestProgressRepository;
import org.kazinduzi.rinjora.data.RinjoraSummaryRepository;
import org.kazinduzi.rinjora.entities.GuestPlayer;
import org.kazinduzi.rinjora.entities.RinjoraSummarySnapshot;
import org.kazinduzi.rinjora.network.AuthTokenStore;

import java.util.Locale;

/**
 * Jewe — "me". The profile tab: shows the server summary ({@code GET /me/summary})
 * with reputation, level, solved riddles, streak, attempts and accuracy, falling
 * back to local guest progress, plus an option to sync guest progress once signed in.
 */
public class JeweFragment extends Fragment {

    private FragmentJeweBinding binding;
    private GuestProgressRepository guestRepository;
    private RinjoraSummaryRepository summaryRepository;

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
        guestRepository = new GuestProgressRepository(requireContext());
        summaryRepository = new RinjoraSummaryRepository(requireContext());

        binding.btnSync.setOnClickListener(v -> syncPending());
    }

    @Override
    public void onResume() {
        super.onResume();
        if (binding == null) {
            return;
        }
        render();
    }

    private void render() {
        boolean loggedIn = AuthTokenStore.get(requireContext()).hasValidToken();

        if (loggedIn) {
            RinjoraSummarySnapshot summary = summaryRepository.getCached();
            if (summary != null) {
                renderSummary(summary);
            } else {
                renderGuest();
            }
            summaryRepository.fetch(new RinjoraSummaryRepository.Callback() {
                @Override
                public void onSuccess(RinjoraSummarySnapshot snapshot) {
                    if (binding != null) {
                        renderSummary(snapshot);
                    }
                }

                @Override
                public void onAuthError() {
                    // stay on whatever we have
                }

                @Override
                public void onError(String message) {
                    // offline: keep cached / guest view
                }
            });
        } else {
            renderGuest();
        }
    }

    private void renderSummary(RinjoraSummarySnapshot s) {
        String name = s.getName();
        binding.tvName.setVisibility(View.VISIBLE);
        binding.tvName.setText(name != null && !name.isEmpty() ? name : "Jewe");
        binding.tvReputation.setText(String.valueOf(s.getReputation()));
        binding.tvLevel.setText(String.valueOf(s.getLevel()));
        binding.tvSolved.setText(String.valueOf(s.getRiddlesSolved()));
        binding.tvStreak.setText(String.valueOf(s.getCurrentStreak()));
        binding.tvAttempts.setText(String.valueOf(s.getTotalAttempts()));
        binding.tvAccuracy.setText(String.format(Locale.getDefault(), "%.0f%%", s.getAccuracy() * 100.0));

        long pending = guestRepository.countPending();
        binding.tvGuestPrompt.setText(pending > 0
                ? pending + " ryandikishijwe riratunze sunkuza."
                : "Urafitse aka konto. Amanota yawe aba ku konto yawe.");
        binding.btnSync.setVisibility(pending > 0 ? View.VISIBLE : View.GONE);
    }

    private void renderGuest() {
        GuestPlayer p = guestRepository.getOrCreatePlayer();
        binding.tvName.setVisibility(View.GONE);

        binding.tvReputation.setText(String.valueOf(p.getTotalPoints()));
        binding.tvSolved.setText(String.valueOf(p.getRiddlesSolved()));
        binding.tvStreak.setText(String.valueOf(p.getCurrentStreak()));
        binding.tvAttempts.setText(String.valueOf(p.getTotalPoints()));
        binding.tvAccuracy.setText("--");
        binding.tvLevel.setText("-");

        boolean loggedIn = guestRepository.isLoggedIn();
        long pending = guestRepository.countPending();
        if (loggedIn) {
            binding.tvGuestPrompt.setText("Urafitse aka konto. Ibiri muri aka kiraya dukoresha."
                    + (pending > 0 ? " " + pending + " ryandikishijwe riratunze sunkuza." : ""));
            binding.btnSync.setVisibility(View.VISIBLE);
        } else {
            binding.tvGuestPrompt.setText("Ukinna nka umushitsi. Ivyawe biba aha ku kiraya."
                    + " Kora aka konto nyuma kugira ubikore ahandi.");
            binding.btnSync.setVisibility(View.GONE);
        }
    }

    private void syncPending() {
        guestRepository.syncPending(new GuestProgressRepository.SyncCallback() {
            @Override
            public void onSynced(int uploaded) {
                Toast.makeText(requireContext(),
                        "Vyasukunzwe " + uploaded + ".", Toast.LENGTH_SHORT).show();
                render();
            }

            @Override
            public void onNotAuthenticated() {
                Toast.makeText(requireContext(), "Kora aka konto mbere yo gusynkuza.",
                        Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onError(String message) {
                Toast.makeText(requireContext(), "Sunkuzo ntivyagenda: " + message,
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
