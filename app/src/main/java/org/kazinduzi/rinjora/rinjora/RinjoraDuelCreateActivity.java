package org.kazinduzi.rinjora.rinjora;

import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.kazinduzi.rinjora.databinding.ActivityRinjoraDuelCreateBinding;
import org.kazinduzi.rinjora.data.RinjoraDuelRepository;
import org.kazinduzi.rinjora.network.AuthTokenStore;
import org.kazinduzi.rinjora.network.dto.DuelDto;

/**
 * Rinjora create-duel screen (plan §7.2): submits an opponent id + riddle id + wager
 * to {@code POST /duels}. Business {@code 422}s (wager beyond reputation, duplicated
 * pending duel, etc.) surface as error toasts.
 */
public class RinjoraDuelCreateActivity extends AppCompatActivity {

    private ActivityRinjoraDuelCreateBinding binding;
    private RinjoraDuelRepository repository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRinjoraDuelCreateBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        if (!AuthTokenStore.get(this).hasValidToken()) {
            finish();
            return;
        }

        repository = new RinjoraDuelRepository(this);
        binding.btnCreate.setOnClickListener(v -> create());
    }

    private void create() {
        long opponentId = parseId(binding.etOpponent.getText().toString());
        long riddleId = parseId(binding.etRiddle.getText().toString());
        if (opponentId <= 0 || riddleId <= 0) {
            Toast.makeText(this, "Enter a valid opponent ID and riddle ID.",
                    Toast.LENGTH_SHORT).show();
            return;
        }
        int wager;
        try {
            wager = Integer.parseInt(binding.etWager.getText().toString().trim());
        } catch (NumberFormatException e) {
            wager = 0;
        }
        if (wager < 0) {
            Toast.makeText(this, "Wager cannot be negative.", Toast.LENGTH_SHORT).show();
            return;
        }

        binding.progressBar.setVisibility(android.view.View.VISIBLE);
        binding.btnCreate.setEnabled(false);

        repository.create(opponentId, riddleId, wager, new RinjoraDuelRepository.Callback<DuelDto>() {
            @Override
            public void onSuccess(DuelDto duel) {
                binding.progressBar.setVisibility(android.view.View.GONE);
                binding.btnCreate.setEnabled(true);
                Toast.makeText(RinjoraDuelCreateActivity.this,
                        "Duel created — waiting for your opponent.",
                        Toast.LENGTH_SHORT).show();
                finish();
            }

            @Override
            public void onAuthError() {
                binding.progressBar.setVisibility(android.view.View.GONE);
                binding.btnCreate.setEnabled(true);
                Toast.makeText(RinjoraDuelCreateActivity.this, "Session expired.",
                        Toast.LENGTH_SHORT).show();
                finish();
            }

            @Override
            public void onError(String message) {
                binding.progressBar.setVisibility(android.view.View.GONE);
                binding.btnCreate.setEnabled(true);
                Toast.makeText(RinjoraDuelCreateActivity.this, message, Toast.LENGTH_LONG).show();
            }
        });
    }

    private long parseId(String s) {
        try {
            return Long.parseLong(s.trim());
        } catch (NumberFormatException e) {
            return -1;
        }
    }
}
