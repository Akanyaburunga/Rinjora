package rw.martinhardware.mymartin.rinjora;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import rw.martinhardware.mymartin.databinding.ActivityRinjoraFavoritesBinding;
import rw.martinhardware.mymartin.databinding.ItemRinjoraFavoritesBinding;
import rw.martinhardware.mymartin.data.RinjoraFavoritesRepository;
import rw.martinhardware.mymartin.data.RinjoraRiddleRepository;
import rw.martinhardware.mymartin.network.AuthTokenStore;
import rw.martinhardware.mymartin.R;
import rw.martinhardware.mymartin.network.dto.RiddleDto;

/**
 * Rinjora favorites screen (plan §6.1): lists {@code GET /me/favorites}, lets the
 * user open a saved riddle for playing, or remove it (heart button → DELETE).
 */
public class RinjoraFavoritesActivity extends AppCompatActivity {

    private ActivityRinjoraFavoritesBinding binding;
    private RinjoraFavoritesRepository favoritesRepository;
    private RinjoraRiddleRepository riddleRepository;
    private FavoritesAdapter adapter;
    private final List<RiddleDto> items = new ArrayList<>();
    private boolean loading;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRinjoraFavoritesBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        if (!AuthTokenStore.get(this).hasValidToken()) {
            goToAuth();
            return;
        }

        favoritesRepository = new RinjoraFavoritesRepository(this);
        riddleRepository = new RinjoraRiddleRepository(this);

        adapter = new FavoritesAdapter();
        binding.recyclerFavorites.setLayoutManager(new LinearLayoutManager(this));
        binding.recyclerFavorites.setAdapter(adapter);
        binding.btnRefresh.setOnClickListener(v -> load());
        binding.swipeRefresh.setOnRefreshListener(this::load);
        binding.swipeRefresh.setColorSchemeResources(R.color.brand_primary, R.color.brand_secondary);

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

        favoritesRepository.fetch(new RinjoraFavoritesRepository.Callback() {
            @Override
            public void onSuccess(List<RiddleDto> favorites) {
                loading = false;
                binding.progressBar.setVisibility(View.GONE);
                binding.swipeRefresh.setRefreshing(false);
                items.clear();
                if (favorites != null) items.addAll(favorites);
                adapter.notifyDataSetChanged();
                binding.tvCount.setText(items.size() + " saved");
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
                Toast.makeText(RinjoraFavoritesActivity.this, message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void openRiddle(RiddleDto r) {
        Intent intent = new Intent(this, RinjoraPlayRiddleActivity.class);
        intent.putExtra("riddle_id", r.getId());
        startActivity(intent);
    }

    private void removeFavorite(final int position) {
        final RiddleDto r = items.get(position);
        riddleRepository.setFavorite(r.getId(), false, new RinjoraRiddleRepository.Callback<Void>() {
            @Override
            public void onSuccess(Void unused) {
                items.remove(position);
                adapter.notifyItemRemoved(position);
                binding.tvCount.setText(items.size() + " saved");
                binding.tvEmpty.setVisibility(items.isEmpty() ? View.VISIBLE : View.GONE);
                Toast.makeText(RinjoraFavoritesActivity.this, "Removed from favorites.",
                        Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onAuthError() {
                goToAuth();
            }

            @Override
            public void onError(String message) {
                Toast.makeText(RinjoraFavoritesActivity.this, message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private class FavoritesAdapter extends RecyclerView.Adapter<FavoritesAdapter.VH> {
        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            ItemRinjoraFavoritesBinding b =
                    ItemRinjoraFavoritesBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
            return new VH(b);
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            final RiddleDto r = items.get(position);
            holder.binding.tvQuestion.setText(r.getQuestion());

            StringBuilder detail = new StringBuilder();
            if (r.getCategory() != null && r.getCategory().getName() != null) {
                detail.append(r.getCategory().getName());
            }
            if (r.getDifficulty() != null && !r.getDifficulty().isEmpty()) {
                if (detail.length() > 0) detail.append(" · ");
                detail.append(cap(r.getDifficulty()));
            }
            if (detail.length() > 0) detail.append(" · ");
            detail.append(r.isSolved() ? "Solved" : "Unsolved");

            holder.binding.tvDetail.setText(detail.toString());
            holder.binding.getRoot().setOnClickListener(v -> openRiddle(r));
            holder.binding.btnRemove.setOnClickListener(v -> removeFavorite(holder.getAdapterPosition()));
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        class VH extends RecyclerView.ViewHolder {
            final ItemRinjoraFavoritesBinding binding;

            VH(ItemRinjoraFavoritesBinding binding) {
                super(binding.getRoot());
                this.binding = binding;
            }
        }
    }

    private String cap(String s) {
        if (s == null || s.isEmpty()) return "";
        return s.substring(0, 1).toUpperCase(java.util.Locale.ROOT) + s.substring(1);
    }

    private void goToAuth() {
        Intent intent = new Intent(this, RinjoraAuthActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
