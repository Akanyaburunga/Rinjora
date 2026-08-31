package org.kazinduzi.rinjora.rinjora;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import org.kazinduzi.rinjora.R;
import org.kazinduzi.rinjora.databinding.ActivityRinjoraProverbsBinding;
import org.kazinduzi.rinjora.databinding.ItemRinjoraProverbBinding;
import org.kazinduzi.rinjora.data.RinjoraProverbRepository;
import org.kazinduzi.rinjora.network.AuthTokenStore;
import org.kazinduzi.rinjora.network.dto.ProverbDto;

/**
 * Heraheza proverb home (parity plan §2.1): lists proverbs from
 * {@code GET /proverbs} and opens a proverb to play it. The detail screen reuses
 * the shared lenient {@code AnswerView}.
 */
public class RinjoraProverbsActivity extends AppCompatActivity {

    private ActivityRinjoraProverbsBinding binding;
    private ProverbAdapter adapter;
    private RinjoraProverbRepository repository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRinjoraProverbsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        repository = new RinjoraProverbRepository(this);

        if (!AuthTokenStore.get(this).hasValidToken()) {
            goToAuth();
            return;
        }

        adapter = new ProverbAdapter();
        binding.recyclerProverbs.setLayoutManager(new LinearLayoutManager(this));
        binding.recyclerProverbs.setAdapter(adapter);

        binding.btnLogout.setOnClickListener(v -> logout());
        binding.swipeRefresh.setOnRefreshListener(this::load);
        binding.swipeRefresh.setColorSchemeResources(R.color.brand_primary, R.color.brand_secondary);

        load();
    }

    private void load() {
        binding.tvEmpty.setVisibility(View.GONE);
        binding.progressBar.setVisibility(View.VISIBLE);

        repository.fetchProverbs(new RinjoraProverbRepository.Callback<List<ProverbDto>>() {
            @Override
            public void onSuccess(List<ProverbDto> proverbs) {
                binding.progressBar.setVisibility(View.GONE);
                binding.swipeRefresh.setRefreshing(false);
                if (proverbs == null || proverbs.isEmpty()) {
                    binding.tvEmpty.setText("No proverbs yet.");
                    binding.tvEmpty.setVisibility(View.VISIBLE);
                } else {
                    adapter.submit(proverbs);
                }
            }

            @Override
            public void onAuthError() {
                binding.progressBar.setVisibility(View.GONE);
                binding.swipeRefresh.setRefreshing(false);
                goToAuth();
            }

            @Override
            public void onError(String message) {
                binding.progressBar.setVisibility(View.GONE);
                binding.swipeRefresh.setRefreshing(false);
                binding.tvEmpty.setText("Couldn’t load proverbs: " + message);
                binding.tvEmpty.setVisibility(View.VISIBLE);
            }
        });
    }

    private class ProverbAdapter extends RecyclerView.Adapter<ProverbAdapter.VH> {
        private final List<ProverbDto> items = new ArrayList<>();

        void submit(List<ProverbDto> proverbs) {
            items.clear();
            items.addAll(proverbs);
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            ItemRinjoraProverbBinding b =
                    ItemRinjoraProverbBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
            return new VH(b);
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            final ProverbDto p = items.get(position);
            holder.binding.tvCategory.setText(p.getCategory() != null ? p.getCategory().getName() : "Imigani");
            holder.binding.tvQuestion.setText(p.getQuestion());
            holder.binding.tvDifficulty.setText(cap(p.getDifficulty()));
            if (p.isSolved()) {
                holder.binding.tvStatus.setText("Solved");
                holder.binding.tvStatus.setVisibility(View.VISIBLE);
            } else {
                holder.binding.tvStatus.setVisibility(View.GONE);
            }
            holder.itemView.setOnClickListener(v -> {
                Intent i = new Intent(RinjoraProverbsActivity.this, RinjoraProverbDetailActivity.class);
                i.putExtra("proverb_id", p.getId());
                startActivity(i);
            });
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        class VH extends RecyclerView.ViewHolder {
            final ItemRinjoraProverbBinding binding;

            VH(ItemRinjoraProverbBinding binding) {
                super(binding.getRoot());
                this.binding = binding;
            }
        }
    }

    private void logout() {
        new org.kazinduzi.rinjora.data.RinjoraAuthRepository(this)
                .logout(new org.kazinduzi.rinjora.data.RinjoraAuthRepository.AuthCallback() {
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
        Intent intent = new Intent(this, RinjoraAuthActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private String cap(String s) {
        if (s == null || s.isEmpty()) return "";
        return s.substring(0, 1).toUpperCase(java.util.Locale.ROOT) + s.substring(1);
    }
}
