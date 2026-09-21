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
import org.kazinduzi.rinjora.data.RinjoraMeRepository;
import org.kazinduzi.rinjora.data.RinjoraRoundRepository;
import org.kazinduzi.rinjora.entities.GuestPlayer;
import org.kazinduzi.rinjora.network.AuthTokenStore;
import org.kazinduzi.rinjora.network.dto.MeDto;
import org.kazinduzi.rinjora.network.dto.RoundHistoryDto;

/**
 * Jewe — "me". The profile tab. Shows the server-owned round stats of the
 * currently logged-in user ({@code GET /api/me} for name/reputation/level/streak,
 * {@code GET /api/games/history} for games/best/total). Guests keep the local
 * guest-progress view with an optional sync button once signed in.
 */
public class JeweFragment extends Fragment {

    private FragmentJeweBinding binding;
    private GuestProgressRepository guestRepository;
    private RinjoraMeRepository meRepository;
    private RinjoraRoundRepository roundRepository;

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
        meRepository = new RinjoraMeRepository(requireContext());
        roundRepository = new RinjoraRoundRepository(requireContext());

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
            binding.tvName.setVisibility(View.VISIBLE);
            binding.tvGuestPrompt.setText("Urafitse aka konto. Amanota yawe aba ku konto yawe.");
            binding.btnSync.setVisibility(View.GONE);
            fetchMe();
            fetchHistory();
        } else {
            renderGuest();
        }
    }

    private void fetchMe() {
        meRepository.fetch(new RinjoraMeRepository.Callback() {
            @Override
            public void onSuccess(MeDto me) {
                if (binding == null) return;
                binding.tvName.setText(me.getName() != null && !me.getName().isEmpty()
                        ? me.getName() : "Jewe");
                int reputation = me.getPoints() != null ? me.getPoints().getReputation() : 0;
                int level = (me.getPoints() != null && me.getPoints().getLevel() != null)
                        ? me.getPoints().getLevel().getLevel() : 0;
                int streak = me.getStreak() != null ? me.getStreak().getCurrent() : 0;
                binding.tvReputation.setText(String.valueOf(reputation));
                binding.tvLevel.setText(String.valueOf(level));
                binding.tvStreak.setText(String.valueOf(streak));
            }

            @Override
            public void onAuthError() {
                // token handling elsewhere; keep last numbers
            }

            @Override
            public void onError(String message) {
                // offline: keep last numbers
            }
        });
    }

    private void fetchHistory() {
        roundRepository.history(new RinjoraRoundRepository.Callback<RoundHistoryDto>() {
            @Override
            public void onSuccess(RoundHistoryDto h) {
                if (binding == null) return;
                binding.tvSolved.setText(String.valueOf(h.getGames()));
                binding.tvAttempts.setText(String.valueOf(h.getTotal()));
                binding.tvAccuracy.setText(String.valueOf(h.getBest()));
            }

            @Override
            public void onAuthError() {
                // keep last numbers
            }

            @Override
            public void onError(String message) {
                // offline: keep last numbers
            }
        });
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