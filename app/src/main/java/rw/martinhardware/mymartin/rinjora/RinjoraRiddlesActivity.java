package rw.martinhardware.mymartin.rinjora;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import rw.martinhardware.mymartin.R;
import rw.martinhardware.mymartin.databinding.ActivityRinjoraRiddlesBinding;
import rw.martinhardware.mymartin.network.ApiEnvelope;
import rw.martinhardware.mymartin.network.RinjoraApi;
import rw.martinhardware.mymartin.network.RinjoraApiClient;
import rw.martinhardware.mymartin.network.dto.RiddleDto;

/**
 * Debug/acceptance screen for the Rinjora API (plan Phase A + Phase B acceptance):
 * after login, pings {@code GET /riddles} with the stored Bearer token and prints JSON.
 * This is a temporary harness replaced by real game screens in later phases.
 */
public class RinjoraRiddlesActivity extends AppCompatActivity {

    private ActivityRinjoraRiddlesBinding binding;
    private RinjoraApi api;

    @Override
    protected void onCreate(android.os.Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRinjoraRiddlesBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        api = RinjoraApiClient.get(this).api();

        binding.btnLogout.setOnClickListener(v -> logout());
        binding.btnRefresh.setOnClickListener(v -> pingRiddles());

        pingRiddles();
    }

    private void pingRiddles() {
        binding.tvMessage.setText("Fetching /riddles ...");
        binding.progressBar.setVisibility(android.view.View.VISIBLE);

        api.riddles().enqueue(new Callback<ApiEnvelope<java.util.List<RiddleDto>>>() {
            @Override
            public void onResponse(@NonNull Call<ApiEnvelope<java.util.List<RiddleDto>>> call,
                                   @NonNull Response<ApiEnvelope<java.util.List<RiddleDto>>> response) {
                binding.progressBar.setVisibility(android.view.View.GONE);
                ApiEnvelope<java.util.List<RiddleDto>> envelope = response.body();
                if (response.isSuccessful() && envelope != null && envelope.isSuccess()) {
                    java.util.List<RiddleDto> riddles = envelope.getData();
                    StringBuilder sb = new StringBuilder("HTTP " + response.code())
                            .append(" · success=true\n\n");
                    if (riddles == null || riddles.isEmpty()) {
                        sb.append("No riddles returned.");
                    } else {
                        sb.append("Riddles (").append(riddles.size()).append("):\n\n");
                        for (RiddleDto r : riddles) {
                            sb.append("• [").append(r.getId()).append("] ")
                                    .append(r.getDifficulty()).append(" — ")
                                    .append(r.getQuestion()).append('\n');
                        }
                    }
                    binding.tvMessage.setText(sb.toString());
                } else {
                    binding.tvMessage.setText("Riddle fetch failed (HTTP " + response.code()
                            + "): " + envelopeMessage(envelope));
                }
            }

            @Override
            public void onFailure(@NonNull Call<ApiEnvelope<java.util.List<RiddleDto>>> call,
                                  @NonNull Throwable t) {
                binding.progressBar.setVisibility(android.view.View.GONE);
                binding.tvMessage.setText("Network error: " + t.getMessage());
            }
        });
    }

    private String envelopeMessage(ApiEnvelope<?> envelope) {
        if (envelope != null && envelope.getMessage() != null) {
            return envelope.getMessage();
        }
        return "Unknown error";
    }

    private void logout() {
        new rw.martinhardware.mymartin.data.RinjoraAuthRepository(this)
                .logout(new rw.martinhardware.mymartin.data.RinjoraAuthRepository.AuthCallback() {
                    @Override
                    public void onSuccess() {
                        goToAuth();
                    }

                    @Override
                    public void onError(String message) {
                        goToAuth();
                    }
                });
    }

    private void goToAuth() {
        android.content.Intent intent = new android.content.Intent(this, RinjoraAuthActivity.class);
        intent.setFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK | android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
