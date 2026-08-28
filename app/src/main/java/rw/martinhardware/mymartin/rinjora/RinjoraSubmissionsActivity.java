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
import java.util.List;
import java.util.Locale;

import rw.martinhardware.mymartin.R;
import rw.martinhardware.mymartin.databinding.ActivityRinjoraSubmissionsBinding;
import rw.martinhardware.mymartin.databinding.ItemRinjoraSubmissionBinding;
import rw.martinhardware.mymartin.data.RinjoraSubmissionRepository;
import rw.martinhardware.mymartin.network.AuthTokenStore;
import rw.martinhardware.mymartin.network.dto.SubmissionDto;

/**
 * Rinjora "my submissions" screen (plan Phase J, §8.2): lists submitted riddles with
 * their review status (pending/approved/rejected) and the rejection reason when
 * applicable, plus a "Contribute" shortcut to the submission form.
 */
public class RinjoraSubmissionsActivity extends AppCompatActivity {

    private ActivityRinjoraSubmissionsBinding binding;
    private RinjoraSubmissionRepository repository;
    private SubmissionAdapter adapter;
    private final List<SubmissionDto> items = new ArrayList<>();
    private boolean loading;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRinjoraSubmissionsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        if (!AuthTokenStore.get(this).hasValidToken()) {
            goToAuth();
            return;
        }

        repository = new RinjoraSubmissionRepository(this);

        adapter = new SubmissionAdapter();
        binding.recyclerSubmissions.setLayoutManager(new LinearLayoutManager(this));
        binding.recyclerSubmissions.setAdapter(adapter);
        binding.btnRefresh.setOnClickListener(v -> load());
        binding.swipeRefresh.setOnRefreshListener(this::load);
        binding.swipeRefresh.setColorSchemeResources(R.color.brand_primary, R.color.brand_secondary);
        binding.btnContribute.setOnClickListener(v ->
                startActivity(new Intent(this, RinjoraContributeActivity.class)));

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

        repository.list(new RinjoraSubmissionRepository.Callback<List<SubmissionDto>>() {
            @Override
            public void onSuccess(List<SubmissionDto> submissions) {
                loading = false;
                binding.progressBar.setVisibility(View.GONE);
                binding.swipeRefresh.setRefreshing(false);
                items.clear();
                if (submissions != null) items.addAll(submissions);
                adapter.notifyDataSetChanged();
                binding.tvCount.setText(String.format(Locale.getDefault(), "%d submission(s)", items.size()));
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
                Toast.makeText(RinjoraSubmissionsActivity.this, message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private class SubmissionAdapter extends RecyclerView.Adapter<SubmissionAdapter.VH> {
        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            ItemRinjoraSubmissionBinding b =
                    ItemRinjoraSubmissionBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
            return new VH(b);
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            SubmissionDto s = items.get(position);
            holder.binding.tvQuestion.setText(s.getQuestion());

            String status = s.getStatus() != null ? s.getStatus() : "";
            holder.binding.tvStatus.setText(status.toUpperCase(Locale.ROOT));
            holder.binding.tvStatus.setTextColor(statusColor(status));

            StringBuilder meta = new StringBuilder();
            if (s.getDifficulty() != null && !s.getDifficulty().isEmpty()) {
                meta.append(cap(s.getDifficulty()));
            }
            if (s.getRiddleType() != null && !s.getRiddleType().isEmpty()) {
                if (meta.length() > 0) meta.append(" · ");
                meta.append(cap(s.getRiddleType().replace("_", " ")));
            }
            if (s.getCreatedAt() != null && !s.getCreatedAt().isEmpty()) {
                if (meta.length() > 0) meta.append(" · ");
                meta.append(String.format(Locale.getDefault(), "%tD", parseDate(s.getCreatedAt())));
            }
            holder.binding.tvMeta.setText(meta.toString());

            String reason = s.getRejectionReason();
            if ("rejected".equalsIgnoreCase(status) && reason != null && !reason.isEmpty()) {
                holder.binding.tvReason.setVisibility(View.VISIBLE);
                holder.binding.tvReason.setText("Reason: " + reason);
            } else {
                holder.binding.tvReason.setVisibility(View.GONE);
            }
        }

        private int statusColor(String status) {
            switch (status) {
                case "approved":
                    return ContextCompat.getColor(RinjoraSubmissionsActivity.this, R.color.brand_success);
                case "rejected":
                    return ContextCompat.getColor(RinjoraSubmissionsActivity.this, R.color.brand_error);
                default:
                    return ContextCompat.getColor(RinjoraSubmissionsActivity.this, R.color.brand_secondary);
            }
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        class VH extends RecyclerView.ViewHolder {
            final ItemRinjoraSubmissionBinding binding;

            VH(ItemRinjoraSubmissionBinding binding) {
                super(binding.getRoot());
                this.binding = binding;
            }
        }
    }

    private String cap(String s) {
        if (s == null || s.isEmpty()) return "";
        return s.substring(0, 1).toUpperCase(Locale.ROOT) + s.substring(1);
    }

    private java.util.Date parseDate(String dateStr) {
        if (dateStr == null) return new java.util.Date();
        try {
            return new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss",
                    Locale.US).parse(dateStr.replace("Z", ""));
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
