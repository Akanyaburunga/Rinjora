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
import java.util.Locale;

import rw.martinhardware.mymartin.R;
import rw.martinhardware.mymartin.data.RinjoraLeaderboardRepository;
import rw.martinhardware.mymartin.databinding.ActivityRinjoraLeaderboardBinding;
import rw.martinhardware.mymartin.databinding.ItemRinjoraLeaderboardBinding;
import rw.martinhardware.mymartin.entities.RinjoraLeaderboardSnapshot;
import rw.martinhardware.mymartin.network.AuthTokenStore;
import rw.martinhardware.mymartin.network.dto.LeaderboardEnvelope;
import rw.martinhardware.mymartin.network.dto.LeaderboardEntryDto;

/**
 * Rinjora leaderboard screen (plan Phase F, §5.1): ranked board with period filter
 * tabs (today/this_week/this_month/this_year/all_time), a highlighted "me" row from
 * the {@code me} block, pagination via the load-more button, and offline render
 * from the last cached envelope.
 */
public class RinjoraLeaderboardActivity extends AppCompatActivity {

    private static final String[] FILTERS = {"today", "this_week", "this_month", "this_year", "all_time"};

    private ActivityRinjoraLeaderboardBinding binding;
    private RinjoraLeaderboardRepository repository;
    private BoardAdapter adapter;
    private String filter = "all_time";
    private int lastPage = 1;
    private int currentPage = 1;
    private long myId = -1;

    private final List<MaterialButton> filterButtons = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRinjoraLeaderboardBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        repository = new RinjoraLeaderboardRepository(this);

        if (!AuthTokenStore.get(this).hasValidToken()) {
            goToAuth();
            return;
        }

        adapter = new BoardAdapter();
        binding.recyclerBoard.setLayoutManager(new LinearLayoutManager(this));
        binding.recyclerBoard.setAdapter(adapter);

        binding.btnRefresh.setOnClickListener(v -> load(filter, 1));
        binding.btnLoadMore.setOnClickListener(v -> load(filter, lastPage + 1));

        buildFilters();
        renderCached();
        load(filter, 1);
    }

    private void buildFilters() {
        for (String f : FILTERS) {
            MaterialButton btn = new MaterialButton(this);
            btn.setText(label(f));
            btn.setAllCaps(false);
            btn.setMinWidth(0);
            btn.setMinHeight(0);
            btn.setPadding(dp(14), dp(6), dp(14), dp(6));
            btn.setTag(f);
            btn.setOnClickListener(v -> {
                filter = (String) v.getTag();
                for (MaterialButton b : filterButtons) {
                    b.setBackgroundResource(b == v ? R.drawable.bg_chip_active : R.drawable.bg_chip);
                    b.setTextColor(getColorCompat(b == v ? R.color.brand_on_primary : R.color.text_secondary));
                }
                load(filter, 1);
            });
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.setMarginEnd(dp(8));
            btn.setLayoutParams(lp);
            boolean active = f.equals(filter);
            btn.setBackgroundResource(active ? R.drawable.bg_chip_active : R.drawable.bg_chip);
            btn.setTextColor(getColorCompat(active ? R.color.brand_on_primary : R.color.text_secondary));
            filterButtons.add(btn);
            binding.filterRow.addView(btn);
        }
    }

    private void renderCached() {
        LeaderboardEnvelope cached = repository.getCachedEnvelope(filter);
        if (cached == null) return;
        renderBoard(cached, false);
    }

    private void load(final String filter, final int page) {
        binding.progressBar.setVisibility(View.VISIBLE);
        binding.btnLoadMore.setVisibility(View.GONE);
        repository.fetch(filter, page, new RinjoraLeaderboardRepository.Callback() {
            @Override
            public void onSuccess(LeaderboardEnvelope envelope) {
                if (binding == null) return;
                binding.progressBar.setVisibility(View.GONE);
                renderBoard(envelope, page > 1);
            }

            @Override
            public void onAuthError() {
                if (binding == null) return;
                binding.progressBar.setVisibility(View.GONE);
                goToAuth();
            }

            @Override
            public void onError(String message) {
                if (binding == null) return;
                binding.progressBar.setVisibility(View.GONE);
                if (!adapter.hasItems()) {
                    binding.tvEmpty.setText("Couldn’t load leaderboard: " + message);
                    binding.tvEmpty.setVisibility(View.VISIBLE);
                }
            }
        });
    }

    private void renderBoard(LeaderboardEnvelope envelope, boolean append) {
        myId = envelope.getMe() != null ? envelope.getMe().getId() : -1;
        renderMe(envelope);
        renderEntries(envelope, append);
        if (envelope.getMeta() != null) {
            lastPage = envelope.getMeta().getLastPage();
            currentPage = envelope.getMeta().getCurrentPage();
            binding.btnLoadMore.setVisibility(currentPage < lastPage ? View.VISIBLE : View.GONE);
        }
    }

    private void renderMe(LeaderboardEnvelope envelope) {
        if (envelope.getMe() != null) {
            binding.meCard.setVisibility(View.VISIBLE);
            binding.tvMeRank.setText("#" + envelope.getMe().getRank());
            binding.tvMeName.setText(envelope.getMe().getName() == null ? "You" : envelope.getMe().getName());
            binding.tvMePoints.setText(envelope.getMe().getPoints() + " pts");
            binding.tvMeDetail.setText(String.format(Locale.getDefault(),
                    "top %d%% of %d players",
                    (int) Math.round(envelope.getMe().getPercentile()),
                    envelope.getMe().getTotalPlayers()));
        } else {
            binding.meCard.setVisibility(View.GONE);
        }
    }

    private void renderEntries(LeaderboardEnvelope envelope, boolean append) {
        if (!append) adapter.clear();
        adapter.submit(envelope.getData());
        binding.tvEmpty.setVisibility(adapter.getItemCount() == 0 ? View.VISIBLE : View.GONE);
        if (adapter.getItemCount() == 0) {
            binding.tvEmpty.setText("No rankings for this period yet.");
        }
    }

    private class BoardAdapter extends RecyclerView.Adapter<BoardAdapter.VH> {
        private final List<LeaderboardEntryDto> items = new ArrayList<>();

        boolean hasItems() {
            return !items.isEmpty();
        }

        void clear() {
            items.clear();
            notifyDataSetChanged();
        }

        void submit(List<LeaderboardEntryDto> entries) {
            items.addAll(entries);
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            ItemRinjoraLeaderboardBinding b = ItemRinjoraLeaderboardBinding.inflate(
                    LayoutInflater.from(parent.getContext()), parent, false);
            return new VH(b);
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            LeaderboardEntryDto e = items.get(position);
            holder.binding.tvRank.setText(String.valueOf(e.getRank()));
            holder.binding.tvName.setText(e.getName() == null ? "" : e.getName());
            holder.binding.tvContrib.setText(String.format(Locale.getDefault(),
                    "%d words · %d meanings",
                    e.getWordsContributed(), e.getMeaningsContributed()));
            holder.binding.tvPoints.setText(e.getPoints() + " pts");

            boolean isMe = myId > 0 && e.getId() == myId;
            holder.itemView.setBackgroundResource(isMe ? R.drawable.bg_chip_active : 0);
            if (isMe) {
                holder.binding.tvName.setTextColor(getColorCompat(R.color.brand_on_primary));
                holder.binding.tvPoints.setTextColor(getColorCompat(R.color.brand_on_primary));
            } else {
                holder.binding.tvName.setTextColor(getColorCompat(R.color.text_primary));
                holder.binding.tvPoints.setTextColor(getColorCompat(R.color.text_primary));
            }
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        class VH extends RecyclerView.ViewHolder {
            final ItemRinjoraLeaderboardBinding binding;

            VH(ItemRinjoraLeaderboardBinding binding) {
                super(binding.getRoot());
                this.binding = binding;
            }
        }
    }

    private int getColorCompat(int res) {
        return androidx.core.content.ContextCompat.getColor(this, res);
    }

    private void goToAuth() {
        Intent intent = new Intent(this, RinjoraAuthActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private String label(String f) {
        switch (f) {
            case "today": return "Today";
            case "this_week": return "Week";
            case "this_month": return "Month";
            case "this_year": return "Year";
            case "all_time":
            default: return "All time";
        }
    }

    private int dp(int v) {
        return Math.round(getResources().getDisplayMetrics().density * v);
    }
}
