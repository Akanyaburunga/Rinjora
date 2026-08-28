package rw.martinhardware.mymartin.rinjora;

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
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import rw.martinhardware.mymartin.R;
import rw.martinhardware.mymartin.databinding.ActivityRinjoraAchievementsBinding;
import rw.martinhardware.mymartin.databinding.ItemRinjoraBadgeBinding;
import rw.martinhardware.mymartin.data.RinjoraAchievementsRepository;
import rw.martinhardware.mymartin.network.AuthTokenStore;
import rw.martinhardware.mymartin.network.dto.AchievementLibraryDto;
import rw.martinhardware.mymartin.network.dto.BadgeDto;

/**
 * Rinjora achievements/badges library screen (plan Phase H, §4.4): shows the full
 * badge library from {@code GET /me/achievements} with earned state and per-badge
 * progress bars. Earned badges are listed first and highlighted.
 */
public class RinjoraAchievementsActivity extends AppCompatActivity {

    private ActivityRinjoraAchievementsBinding binding;
    private RinjoraAchievementsRepository repository;
    private BadgeAdapter adapter;
    private final List<BadgeDto> items = new ArrayList<>();
    private boolean loading;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRinjoraAchievementsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        if (!AuthTokenStore.get(this).hasValidToken()) {
            goToAuth();
            return;
        }

        repository = new RinjoraAchievementsRepository(this);

        adapter = new BadgeAdapter();
        binding.recyclerAchievements.setLayoutManager(new LinearLayoutManager(this));
        binding.recyclerAchievements.setAdapter(adapter);
        binding.btnRefresh.setOnClickListener(v -> load());

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

        repository.fetch(new RinjoraAchievementsRepository.Callback() {
            @Override
            public void onSuccess(AchievementLibraryDto library) {
                loading = false;
                binding.progressBar.setVisibility(View.GONE);
                items.clear();
                if (library != null && library.getAchievements() != null) {
                    items.addAll(library.getAchievements());
                }
                sort(items);
                adapter.notifyDataSetChanged();

                int earned = library != null ? library.getEarnedCount() : 0;
                int total = library != null ? library.getTotal() : items.size();
                binding.tvCount.setText(String.format(Locale.getDefault(), "%d of %d earned", earned, total));
                binding.tvEmpty.setVisibility(items.isEmpty() ? View.VISIBLE : View.GONE);
            }

            @Override
            public void onAuthError() {
                loading = false;
                binding.progressBar.setVisibility(View.GONE);
                goToAuth();
            }

            @Override
            public void onError(String message) {
                loading = false;
                binding.progressBar.setVisibility(View.GONE);
                Toast.makeText(RinjoraAchievementsActivity.this, message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void sort(List<BadgeDto> badges) {
        Collections.sort(badges, new Comparator<BadgeDto>() {
            @Override
            public int compare(BadgeDto a, BadgeDto b) {
                if (a.isEarned() != b.isEarned()) {
                    return a.isEarned() ? -1 : 1;
                }
                String ca = a.getCategory() != null ? a.getCategory() : "";
                String cb = b.getCategory() != null ? b.getCategory() : "";
                int byCat = ca.compareToIgnoreCase(cb);
                if (byCat != 0) return byCat;
                String na = a.getName() != null ? a.getName() : "";
                String nb = b.getName() != null ? b.getName() : "";
                return na.compareToIgnoreCase(nb);
            }
        });
    }

    private class BadgeAdapter extends RecyclerView.Adapter<BadgeAdapter.VH> {
        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            ItemRinjoraBadgeBinding b =
                    ItemRinjoraBadgeBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
            return new VH(b);
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            BadgeDto badge = items.get(position);
            boolean earned = badge.isEarned();

            holder.binding.tvName.setText(badge.getName());
            holder.binding.tvName.setTextColor(ContextCompat.getColor(
                    RinjoraAchievementsActivity.this,
                    earned ? R.color.text_primary : R.color.text_secondary));
            holder.binding.tvCategory.setText(presentableCategory(badge.getCategory()));
            holder.binding.tvDescription.setText(badge.getDescription() != null ? badge.getDescription() : "");

            if (earned) {
                holder.binding.tvEarned.setText("EARNED");
                holder.binding.tvEarned.setVisibility(View.VISIBLE);
                holder.binding.ivMarker.setBackgroundColor(ContextCompat.getColor(
                        RinjoraAchievementsActivity.this, R.color.brand_success));
            } else {
                holder.binding.tvEarned.setText("");
                holder.binding.tvEarned.setVisibility(View.INVISIBLE);
                holder.binding.ivMarker.setBackgroundColor(ContextCompat.getColor(
                        RinjoraAchievementsActivity.this, R.color.brand_outline));
            }

            int goal = badge.getGoal();
            int progress = Math.max(0, badge.getProgress());
            if (goal > 0) {
                int pct = (int) Math.min(100, Math.round(100.0 * progress / goal));
                holder.binding.progress.setProgress(pct);
                holder.binding.tvProgress.setText(String.format(Locale.getDefault(), "%d / %d", progress, goal));
                holder.binding.progress.setVisibility(View.VISIBLE);
                holder.binding.tvProgress.setVisibility(View.VISIBLE);
            } else {
                holder.binding.progress.setVisibility(View.GONE);
                holder.binding.tvProgress.setVisibility(View.INVISIBLE);
            }
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        class VH extends RecyclerView.ViewHolder {
            final ItemRinjoraBadgeBinding binding;

            VH(ItemRinjoraBadgeBinding binding) {
                super(binding.getRoot());
                this.binding = binding;
            }
        }
    }

    private String presentableCategory(String category) {
        if (category == null || category.isEmpty()) return "";
        return category.replace("_", " ");
    }

    private void goToAuth() {
        Intent intent = new Intent(this, RinjoraAuthActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
