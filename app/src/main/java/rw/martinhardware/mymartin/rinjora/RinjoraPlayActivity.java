package rw.martinhardware.mymartin.rinjora;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;

import rw.martinhardware.mymartin.R;
import rw.martinhardware.mymartin.databinding.ActivityRinjoraPlayBinding;
import rw.martinhardware.mymartin.databinding.ItemRinjoraRiddleBinding;
import rw.martinhardware.mymartin.network.AuthTokenStore;
import rw.martinhardware.mymartin.network.dto.RiddleDto;
import rw.martinhardware.mymartin.data.RinjoraCatalogRepository;

/**
 * Rinjora play list screen (plan Phase D): lists {@code GET /riddles} with
 * difficulty/type filters and open a riddle to play it. The previous debug
 * harness is removed here — this is the real game screen.
 */
public class RinjoraPlayActivity extends AppCompatActivity {

    private static final String[] FILTERS = {"all", "easy", "medium", "hard"};

    private ActivityRinjoraPlayBinding binding;
    private RiddleAdapter adapter;
    private String difficulty = "all";
    private RinjoraCatalogRepository catalog;

    private final List<MaterialButton> filterButtons = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRinjoraPlayBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        catalog = new RinjoraCatalogRepository(this);

        if (!AuthTokenStore.get(this).hasValidToken()) {
            goToAuth();
            return;
        }

        adapter = new RiddleAdapter();
        binding.recyclerRiddles.setLayoutManager(new LinearLayoutManager(this));
        binding.recyclerRiddles.setAdapter(adapter);

        binding.btnLogout.setOnClickListener(v -> logout());
        binding.btnHistory.setOnClickListener(v ->
                startActivity(new Intent(this, RinjoraHistoryActivity.class)));

        binding.swipeRefresh.setOnRefreshListener(this::refreshFromNetwork);
        binding.swipeRefresh.setColorSchemeResources(R.color.brand_primary, R.color.brand_secondary);

        buildFilters();

        // Offline-first: render whatever is cached instantly, then refresh.
        if (!catalog.getCachedRiddles().isEmpty() || !catalog.getCachedCategories().isEmpty()) {
            renderCachedScreen();
        }
        refreshFromNetwork();
    }

    private void renderCachedScreen() {
        java.util.List<RiddleDto> cached = filterLocally(catalog.getCachedRiddles());
        if (cached.isEmpty()) {
            binding.recyclerRiddles.setVisibility(View.VISIBLE);
            binding.tvEmpty.setText("No riddles match this filter.");
            binding.tvEmpty.setVisibility(View.VISIBLE);
            return;
        }
        adapter.clear();
        adapter.submit(new ArrayList<>(cached));
        binding.tvEmpty.setVisibility(View.GONE);
    }

    private java.util.List<RiddleDto> filterLocally(java.util.List<RiddleDto> riddles) {
        java.util.List<RiddleDto> out = new ArrayList<>();
        String d = "all".equals(difficulty) ? null : difficulty;
        for (RiddleDto r : riddles) {
            if (d != null && !d.equalsIgnoreCase(r.getDifficulty())) continue;
            out.add(r);
        }
        return out;
    }

    private void refreshFromNetwork() {
        loadRiddles();
    }

    private void buildFilters() {
        for (String label : FILTERS) {
            MaterialButton btn = new MaterialButton(this);
            btn.setText(cap(label));
            btn.setAllCaps(false);
            btn.setMinWidth(0);
            btn.setMinHeight(0);
            btn.setPadding(dp(14), dp(6), dp(14), dp(6));
            btn.setTag(label);
            btn.setOnClickListener(v -> {
                difficulty = (String) v.getTag();
                for (MaterialButton b : filterButtons) {
                    b.setBackgroundResource(b == v ? R.drawable.bg_chip_active : R.drawable.bg_chip);
                    b.setTextColor(getColor(b == v ? R.color.brand_on_primary : R.color.text_secondary));
                }
                renderCachedScreen();
                refreshFromNetwork();
            });
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.setMarginEnd(dp(8));
            btn.setLayoutParams(lp);
            btn.setBackgroundResource(label.equals("all") ? R.drawable.bg_chip_active : R.drawable.bg_chip);
            btn.setTextColor(getColor(label.equals("all") ? R.color.brand_on_primary : R.color.text_secondary));
            filterButtons.add(btn);
            binding.filterRow.addView(btn);
        }
    }

    private void loadRiddles() {
        binding.tvEmpty.setVisibility(View.GONE);
        binding.progressBar.setVisibility(View.VISIBLE);

        // Populate the catalog cache every refresh so the list is available offline.
        catalog.fetchRiddles(new RinjoraCatalogRepository.Callback() {
            @Override
            public void onSuccess() {
                binding.progressBar.setVisibility(View.GONE);
                binding.swipeRefresh.setRefreshing(false);
                renderCachedScreen();
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
                // Offline fallback: reuse the cached list, filtered locally.
                java.util.List<RiddleDto> cached = filterLocally(catalog.getCachedRiddles());
                if (!cached.isEmpty()) {
                    adapter.clear();
                    adapter.submit(new ArrayList<>(cached));
                    binding.tvEmpty.setVisibility(View.GONE);
                } else {
                    binding.tvEmpty.setText("Offline — no cached riddles yet.\n" + message);
                    binding.tvEmpty.setVisibility(View.VISIBLE);
                }
            }
        });

        // Also warm the category cache so the play screen works fully offline.
        catalog.fetchCategories(new RinjoraCatalogRepository.Callback() {
            @Override
            public void onSuccess() {
            }

            @Override
            public void onAuthError() {
            }

            @Override
            public void onError(String message) {
            }
        });
    }

    private class RiddleAdapter extends RecyclerView.Adapter<RiddleAdapter.VH> {
        private final List<RiddleDto> items = new ArrayList<>();

        void clear() {
            items.clear();
            notifyDataSetChanged();
        }

        void submit(List<RiddleDto> riddles) {
            items.addAll(riddles);
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            ItemRinjoraRiddleBinding b =
                    ItemRinjoraRiddleBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
            return new VH(b);
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            final RiddleDto r = items.get(position);
            holder.binding.tvCategory.setText(r.getCategory() != null ? r.getCategory().getName() : "Riddle");
            holder.binding.tvQuestion.setText(r.getQuestion());
            holder.binding.tvDifficulty.setText(cap(r.getDifficulty()));
            if (r.isSolved()) {
                holder.binding.tvStatus.setText("Solved");
                holder.binding.tvStatus.setVisibility(View.VISIBLE);
            } else {
                holder.binding.tvStatus.setVisibility(View.GONE);
            }
            holder.itemView.setOnClickListener(v -> {
                Intent i = new Intent(RinjoraPlayActivity.this, RinjoraPlayRiddleActivity.class);
                i.putExtra("riddle_id", r.getId());
                startActivity(i);
            });
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        class VH extends RecyclerView.ViewHolder {
            final ItemRinjoraRiddleBinding binding;

            VH(ItemRinjoraRiddleBinding binding) {
                super(binding.getRoot());
                this.binding = binding;
            }
        }
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
        Intent intent = new Intent(this, RinjoraAuthActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private String cap(String s) {
        if (s == null || s.isEmpty()) return "";
        return s.substring(0, 1).toUpperCase(java.util.Locale.ROOT) + s.substring(1);
    }

    private int dp(int v) {
        return Math.round(getResources().getDisplayMetrics().density * v);
    }
}
