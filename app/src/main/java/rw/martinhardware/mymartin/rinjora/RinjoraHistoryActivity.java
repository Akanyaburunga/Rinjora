package rw.martinhardware.mymartin.rinjora;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import rw.martinhardware.mymartin.R;
import rw.martinhardware.mymartin.databinding.ActivityRinjoraHistoryBinding;
import rw.martinhardware.mymartin.databinding.ItemRinjoraHistoryBinding;
import rw.martinhardware.mymartin.network.ApiEnvelope;
import rw.martinhardware.mymartin.network.AuthTokenStore;
import rw.martinhardware.mymartin.network.RinjoraApi;
import rw.martinhardware.mymartin.network.RinjoraApiClient;
import rw.martinhardware.mymartin.network.dto.HistoryEntryDto;
import rw.martinhardware.mymartin.network.dto.HistoryStatsDto;

/**
 * Rinjora attempt history + stats screen (plan Phase D §2.10/§2.11):
 * {@code GET /riddles/history} (paged) and {@code GET /riddles/history/stats}.
 */
public class RinjoraHistoryActivity extends AppCompatActivity {

    private ActivityRinjoraHistoryBinding binding;
    private RinjoraApi api;
    private HistoryAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRinjoraHistoryBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        api = RinjoraApiClient.get(this).api();

        if (!AuthTokenStore.get(this).hasValidToken()) {
            goToAuth();
            return;
        }

        adapter = new HistoryAdapter();
        binding.recyclerHistory.setLayoutManager(new LinearLayoutManager(this));
        binding.recyclerHistory.setAdapter(adapter);
        binding.btnRefresh.setOnClickListener(v -> load());

        load();
    }

    private void load() {
        binding.progressBar.setVisibility(View.VISIBLE);
        binding.tvEmpty.setVisibility(View.GONE);

        api.historyStats().enqueue(new retrofit2.Callback<ApiEnvelope<HistoryStatsDto>>() {
            @Override
            public void onResponse(@NonNull retrofit2.Call<ApiEnvelope<HistoryStatsDto>> call,
                                   @NonNull retrofit2.Response<ApiEnvelope<HistoryStatsDto>> response) {
                ApiEnvelope<HistoryStatsDto> envelope = response.body();
                if (response.isSuccessful() && envelope != null && envelope.isSuccess()
                        && envelope.getData() != null) {
                    renderStats(envelope.getData());
                } else if (response.code() == 401) {
                    goToAuth();
                }
            }

            @Override
            public void onFailure(@NonNull retrofit2.Call<ApiEnvelope<HistoryStatsDto>> call,
                                  @NonNull Throwable t) {
                // stats are best-effort; history list is the primary content
            }
        });

        api.history(15).enqueue(new retrofit2.Callback<ApiEnvelope<List<HistoryEntryDto>>>() {
            @Override
            public void onResponse(@NonNull retrofit2.Call<ApiEnvelope<List<HistoryEntryDto>>> call,
                                   @NonNull retrofit2.Response<ApiEnvelope<List<HistoryEntryDto>>> response) {
                binding.progressBar.setVisibility(View.GONE);
                ApiEnvelope<List<HistoryEntryDto>> envelope = response.body();
                if (response.isSuccessful() && envelope != null && envelope.isSuccess()) {
                    List<HistoryEntryDto> entries = envelope.getData();
                    if (entries == null || entries.isEmpty()) {
                        binding.tvEmpty.setText("No attempts yet.");
                        binding.tvEmpty.setVisibility(View.VISIBLE);
                    } else {
                        adapter.submit(entries);
                    }
                } else if (response.code() == 401) {
                    goToAuth();
                } else {
                    binding.tvEmpty.setText("Couldn’t load history: "
                            + envelopeMessage(envelope));
                    binding.tvEmpty.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onFailure(@NonNull retrofit2.Call<ApiEnvelope<List<HistoryEntryDto>>> call,
                                  @NonNull Throwable t) {
                binding.progressBar.setVisibility(View.GONE);
                binding.tvEmpty.setText("Network error: " + t.getMessage());
                binding.tvEmpty.setVisibility(View.VISIBLE);
            }
        });
    }

    private void renderStats(HistoryStatsDto s) {
        java.util.Locale loc = java.util.Locale.getDefault();
        binding.tvStats.setText(String.format(loc,
                "%d attempts · %d solved · %d unique · %d%% accurate",
                s.getTotalAttempts(), s.getRiddlesSolved(),
                s.getUniqueRiddles(), (int) Math.round(s.getAccuracy())));
    }

    private class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.VH> {
        private final List<HistoryEntryDto> items = new ArrayList<>();

        void submit(List<HistoryEntryDto> entries) {
            items.addAll(entries);
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            ItemRinjoraHistoryBinding b =
                    ItemRinjoraHistoryBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
            return new VH(b);
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            HistoryEntryDto e = items.get(position);
            String question = e.getRiddle() != null ? e.getRiddle().getQuestion()
                    : ("Riddle #" + (e.getRiddle() != null ? e.getRiddle().getId() : e.getId()));
            holder.binding.tvQuestion.setText(question);

            StringBuilder detail = new StringBuilder();
            detail.append("Your answer: “").append(e.getSubmittedAnswer() == null ? "" : e.getSubmittedAnswer()).append("”");
            java.util.Locale loc = java.util.Locale.getDefault();
            detail.append(String.format(loc, "  ·  %tD", parseDate(e.getAttemptedAt())));

            int dotColor;
            if (e.isCorrect()) {
                separate(detail, "Correct");
                if (e.isRewarded()) separate(detail, "+points");
                dotColor = androidx.core.content.ContextCompat.getColor(
                        RinjoraHistoryActivity.this, R.color.brand_success);
            } else {
                separate(detail, "Incorrect");
                dotColor = androidx.core.content.ContextCompat.getColor(
                        RinjoraHistoryActivity.this, R.color.brand_error);
            }

            holder.binding.tvDetail.setText(detail.toString());
            holder.binding.ivMarker.setBackgroundColor(dotColor);
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        class VH extends RecyclerView.ViewHolder {
            final ItemRinjoraHistoryBinding binding;

            VH(ItemRinjoraHistoryBinding binding) {
                super(binding.getRoot());
                this.binding = binding;
            }
        }
    }

    private void separate(StringBuilder sb, String label) {
        if (sb.length() > 0) sb.append(" · ");
        sb.append(label);
    }

    private java.util.Date parseDate(String attemptedAt) {
        if (attemptedAt == null) return new java.util.Date();
        try {
            return new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss",
                    java.util.Locale.US)
                    .parse(attemptedAt.replace("Z", ""));
        } catch (Exception e) {
            try {
                return new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss",
                        java.util.Locale.US).parse(attemptedAt);
            } catch (Exception e2) {
                return new java.util.Date();
            }
        }
    }

    private void goToAuth() {
        android.content.Intent intent = new android.content.Intent(this, RinjoraAuthActivity.class);
        intent.setFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK
                | android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private String envelopeMessage(ApiEnvelope<?> envelope) {
        return envelope != null && envelope.getMessage() != null ? envelope.getMessage() : "Unknown error";
    }
}
