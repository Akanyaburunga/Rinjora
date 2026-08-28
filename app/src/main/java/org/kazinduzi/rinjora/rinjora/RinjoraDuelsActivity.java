package org.kazinduzi.rinjora.rinjora;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.kazinduzi.rinjora.R;
import org.kazinduzi.rinjora.databinding.ActivityRinjoraDuelsBinding;
import org.kazinduzi.rinjora.databinding.ItemRinjoraDuelBinding;
import org.kazinduzi.rinjora.data.RinjoraDuelRepository;
import org.kazinduzi.rinjora.network.AuthTokenStore;
import org.kazinduzi.rinjora.network.dto.DuelDto;
import org.kazinduzi.rinjora.network.dto.DuelUserDto;

/**
 * Rinjora duels (PvP) list screen (plan Phase I, §7.1 + §7.7): shows every duel I
 * am party to with status-aware actions — Accept/Decline for a pending incoming,
 * "Waiting" for an outgoing pending, Open to play an accepted duel, a winner
 * banner when completed, and an inactive row for declined/expired.
 */
public class RinjoraDuelsActivity extends AppCompatActivity {

    private ActivityRinjoraDuelsBinding binding;
    private RinjoraDuelRepository repository;
    private DuelAdapter adapter;
    private final List<DuelDto> items = new ArrayList<>();
    private boolean loading;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRinjoraDuelsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        if (!AuthTokenStore.get(this).hasValidToken()) {
            goToAuth();
            return;
        }

        repository = new RinjoraDuelRepository(this);

        adapter = new DuelAdapter();
        binding.recyclerDuels.setLayoutManager(new LinearLayoutManager(this));
        binding.recyclerDuels.setAdapter(adapter);
        binding.btnRefresh.setOnClickListener(v -> load());
        binding.swipeRefresh.setOnRefreshListener(this::load);
        binding.swipeRefresh.setColorSchemeResources(R.color.brand_primary, R.color.brand_secondary);
        binding.btnNew.setOnClickListener(v ->
                startActivity(new Intent(this, RinjoraDuelCreateActivity.class)));

        load();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!loading) load();
    }

    private void load() {
        if (loading) return;
        loading = true;
        binding.progressBar.setVisibility(View.VISIBLE);
        binding.tvEmpty.setVisibility(View.GONE);

        repository.fetchDuels(new RinjoraDuelRepository.Callback<List<DuelDto>>() {
            @Override
            public void onSuccess(List<DuelDto> duels) {
                loading = false;
                binding.progressBar.setVisibility(View.GONE);
                binding.swipeRefresh.setRefreshing(false);
                items.clear();
                if (duels != null) items.addAll(duels);
                adapter.notifyDataSetChanged();
                binding.tvCount.setText(String.format(Locale.getDefault(), "%d duel(s)", items.size()));
                binding.tvEmpty.setVisibility(items.isEmpty() ? View.VISIBLE : View.GONE);
            }

            @Override
            public void onAuthError() {
                loading = false;
                binding.progressBar.setVisibility(View.GONE);
                binding.swipeRefresh.setRefreshing(false);
                goToAuth();
            }

            @Override
            public void onError(String message) {
                loading = false;
                binding.progressBar.setVisibility(View.GONE);
                binding.swipeRefresh.setRefreshing(false);
                Toast.makeText(RinjoraDuelsActivity.this, message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void acceptDuel(final int position) {
        final DuelDto duel = items.get(position);
        repository.accept(duel.getId(), new RinjoraDuelRepository.Callback<DuelDto>() {
            @Override
            public void onSuccess(DuelDto updated) {
                if (updated != null) {
                    items.set(position, updated);
                    adapter.notifyItemChanged(position);
                } else {
                    load();
                }
                Toast.makeText(RinjoraDuelsActivity.this, "Duel accepted.", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onAuthError() {
                goToAuth();
            }

            @Override
            public void onError(String message) {
                Toast.makeText(RinjoraDuelsActivity.this, message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void declineDuel(final int position) {
        final DuelDto duel = items.get(position);
        repository.decline(duel.getId(), new RinjoraDuelRepository.Callback<DuelDto>() {
            @Override
            public void onSuccess(DuelDto updated) {
                if (updated != null) {
                    items.set(position, updated);
                    adapter.notifyItemChanged(position);
                } else {
                    load();
                }
                Toast.makeText(RinjoraDuelsActivity.this, "Duel declined.", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onAuthError() {
                goToAuth();
            }

            @Override
            public void onError(String message) {
                Toast.makeText(RinjoraDuelsActivity.this, message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void openDuel(DuelDto duel) {
        Intent intent = new Intent(this, RinjoraDuelDetailActivity.class);
        intent.putExtra("duel_id", duel.getId());
        intent.putExtra("direction", duel.getDirection());
        startActivity(intent);
    }

    private class DuelAdapter extends RecyclerView.Adapter<DuelAdapter.VH> {
        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            ItemRinjoraDuelBinding b =
                    ItemRinjoraDuelBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
            return new VH(b);
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            final DuelDto duel = items.get(position);
            render(holder, duel, position);
        }

        private void render(VH holder, final DuelDto duel, final int position) {
            DuelUserDto other = duel.isOutgoing() ? duel.getOpponent() : duel.getInitiator();
            String otherName = other != null && other.getName() != null ? other.getName() : "?";
            holder.binding.tvVs.setText("vs " + otherName);

            String status = duel.getStatus() != null ? duel.getStatus() : "";
            holder.binding.tvStatus.setText(status.toUpperCase(Locale.ROOT));
            holder.binding.tvStatus.setTextColor(statusColor(status));

            String riddle = duel.getRiddle() != null && duel.getRiddle().getQuestion() != null
                    ? duel.getRiddle().getQuestion() : "Riddle";
            holder.binding.tvRiddle.setText(riddle);

            StringBuilder meta = new StringBuilder();
            meta.append("Wager ").append(duel.getWager());
            String created = duel.getCreatedAt();
            if (created != null && !created.isEmpty()) {
                meta.append(" · ").append(String.format(Locale.getDefault(), "%tD", parseDate(created)));
            }
            holder.binding.tvMeta.setText(meta.toString());

            holder.binding.tvWaiting.setVisibility(View.GONE);
            holder.binding.llActions.setVisibility(View.GONE);

            switch (status) {
                case "pending":
                    if (duel.isOutgoing()) {
                        holder.binding.tvWaiting.setVisibility(View.VISIBLE);
                        holder.binding.tvWaiting.setText("Waiting for the opponent to accept…");
                    } else {
                        holder.binding.llActions.setVisibility(View.VISIBLE);
                        holder.binding.btnOpen.setVisibility(View.GONE);
                        holder.binding.btnAccept.setOnClickListener(v -> acceptDuel(position));
                        holder.binding.btnDecline.setOnClickListener(v -> declineDuel(position));
                    }
                    break;
                case "accepted":
                    holder.binding.llActions.setVisibility(View.VISIBLE);
                    holder.binding.btnAccept.setVisibility(View.GONE);
                    holder.binding.btnDecline.setVisibility(View.GONE);
                    holder.binding.btnOpen.setOnClickListener(v -> openDuel(duel));
                    break;
                case "completed":
                    holder.binding.tvWaiting.setVisibility(View.VISIBLE);
                    String winner = winnerLabel(duel);
                    holder.binding.tvWaiting.setText(winner);
                    holder.binding.tvWaiting.setTextColor(ContextCompat.getColor(
                            RinjoraDuelsActivity.this, R.color.brand_primary));
                    break;
                default: // declined, expired
                    holder.binding.tvWaiting.setVisibility(View.VISIBLE);
                    holder.binding.tvWaiting.setText(status.toUpperCase(Locale.ROOT));
                    break;
            }
        }

        private String winnerLabel(DuelDto duel) {
            Long winnerId = duel.getWinnerId();
            DuelUserDto other = duel.isOutgoing() ? duel.getOpponent() : duel.getInitiator();
            String otherName = other != null && other.getName() != null ? other.getName() : "?";
            if (winnerId == null || winnerId == 0) {
                return "Resolved — no winner.";
            }
            if (other != null && other.getId() == winnerId) {
                return "Won by " + otherName + " (+" + duel.getWager() + " rep)";
            }
            // otherwise I won
            return "You won! (+" + duel.getWager() + " rep)";
        }

        private int statusColor(String status) {
            switch (status) {
                case "accepted":
                    return ContextCompat.getColor(RinjoraDuelsActivity.this, R.color.brand_primary);
                case "completed":
                    return ContextCompat.getColor(RinjoraDuelsActivity.this, R.color.brand_success);
                case "pending":
                    return ContextCompat.getColor(RinjoraDuelsActivity.this, R.color.brand_secondary);
                default:
                    return ContextCompat.getColor(RinjoraDuelsActivity.this, R.color.text_muted);
            }
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        class VH extends RecyclerView.ViewHolder {
            final ItemRinjoraDuelBinding binding;

            VH(ItemRinjoraDuelBinding binding) {
                super(binding.getRoot());
                this.binding = binding;
            }
        }
    }

    private java.util.Date parseDate(String dateStr) {
        if (dateStr == null) return new java.util.Date();
        try {
            return new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss",
                    java.util.Locale.US).parse(dateStr.replace("Z", ""));
        } catch (Exception e) {
            return new java.util.Date();
        }
    }

    private void goToAuth() {
        Intent intent = new Intent(this, RinjoraAuthActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
