package org.kazinduzi.rinjora.rinjora;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import org.kazinduzi.rinjora.R;
import org.kazinduzi.rinjora.databinding.ActivityRinjoraHistoryBinding;
import org.kazinduzi.rinjora.databinding.ItemRinjoraHistoryBinding;
import org.kazinduzi.rinjora.databinding.ItemRinjoraHistoryHeaderBinding;
import org.kazinduzi.rinjora.data.RinjoraJokeRepository;
import org.kazinduzi.rinjora.data.RinjoraProverbRepository;
import org.kazinduzi.rinjora.network.ApiEnvelope;
import org.kazinduzi.rinjora.network.AuthTokenStore;
import org.kazinduzi.rinjora.network.RinjoraApi;
import org.kazinduzi.rinjora.network.RinjoraApiClient;
import org.kazinduzi.rinjora.network.dto.HistoryEntryDto;
import org.kazinduzi.rinjora.network.dto.HistoryStatsDto;
import org.kazinduzi.rinjora.network.dto.JokeHistoryEntryDto;
import org.kazinduzi.rinjora.network.dto.JokeHistoryStatsDto;
import org.kazinduzi.rinjora.network.dto.ProverbHistoryEntryDto;
import org.kazinduzi.rinjora.network.dto.ProverbHistoryStatsDto;

/**
 * Rinjora attempt history extended to three per-mode sections (parity plan §4.1):
 * Sokwe (riddles), Heraheza (proverbs) and Tujajure (jokes). Each section shows a
 * header with its stats, followed by that mode's recent attempts. If a mode's history
 * stats endpoint is unavailable (backend pending), we fall back to a locally cached
 * solved count so the section still renders.
 */
public class RinjoraHistoryActivity extends AppCompatActivity {

    private static final int TYPE_HEADER = 0;
    private static final int TYPE_ENTRY = 1;
    private static final int TOTAL_CALLS = 6; // 3 modes x (stats + entries)

    private ActivityRinjoraHistoryBinding binding;
    private RinjoraApi api;
    private HistoryAdapter adapter;

    private final ResultHolder result = new ResultHolder();
    private boolean rendered;
    private int pendingCalls = TOTAL_CALLS;

    private static class ResultHolder {
        String riddleStats = "—";
        String proverbStats = "—";
        String jokeStats = "—";
        List<DisplayItem> riddles = new ArrayList<>();
        List<DisplayItem> proverbs = new ArrayList<>();
        List<DisplayItem> jokes = new ArrayList<>();
    }

    private static class DisplayItem {
        final int type;
        final String title;
        final String stats;
        final String detail;
        final int dotColor;

        DisplayItem(String title, String stats) {
            this.type = TYPE_HEADER;
            this.title = title;
            this.stats = stats;
            this.detail = "";
            this.dotColor = 0;
        }

        DisplayItem(String title, String detail, int dotColor) {
            this.type = TYPE_ENTRY;
            this.title = title;
            this.stats = "";
            this.detail = detail;
            this.dotColor = dotColor;
        }
    }

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
        binding.swipeRefresh.setOnRefreshListener(this::load);
        binding.swipeRefresh.setColorSchemeResources(R.color.brand_primary, R.color.brand_secondary);

        binding.tvStats.setText("Loading…");
        load();
    }

    private void load() {
        rendered = false;
        pendingCalls = TOTAL_CALLS;
        binding.progressBar.setVisibility(View.VISIBLE);
        binding.tvEmpty.setVisibility(View.GONE);
        result.riddleStats = "—";
        result.proverbStats = "—";
        result.jokeStats = "—";
        result.riddles.clear();
        result.proverbs.clear();
        result.jokes.clear();
        adapter.clear();

        loadRiddles();
        loadProverbs();
        loadJokes();
    }

    // ------------------------------------------------------------------
    // Sokwe (riddles)
    // ------------------------------------------------------------------

    private void loadRiddles() {
        api.historyStats().enqueue(new retrofit2.Callback<ApiEnvelope<HistoryStatsDto>>() {
            @Override
            public void onResponse(@NonNull retrofit2.Call<ApiEnvelope<HistoryStatsDto>> call,
                                   @NonNull retrofit2.Response<ApiEnvelope<HistoryStatsDto>> response) {
                ApiEnvelope<HistoryStatsDto> envelope = response.body();
                if (response.isSuccessful() && envelope != null && envelope.isSuccess()
                        && envelope.getData() != null) {
                    result.riddleStats = statsLine(envelope.getData().getTotalAttempts(),
                            envelope.getData().getRiddlesSolved(), envelope.getData().getAccuracy());
                } else if (response.code() == 401) {
                    goToAuth();
                    return;
                }
                maybeRender();
            }

            @Override
            public void onFailure(@NonNull retrofit2.Call<ApiEnvelope<HistoryStatsDto>> call,
                                  @NonNull Throwable t) {
                maybeRender();
            }
        });

        api.history(15).enqueue(new retrofit2.Callback<ApiEnvelope<List<HistoryEntryDto>>>() {
            @Override
            public void onResponse(@NonNull retrofit2.Call<ApiEnvelope<List<HistoryEntryDto>>> call,
                                   @NonNull retrofit2.Response<ApiEnvelope<List<HistoryEntryDto>>> response) {
                List<HistoryEntryDto> entries = response.isSuccessful()
                        && response.body() != null ? response.body().getData() : null;
                result.riddles = entries == null
                        ? new ArrayList<DisplayItem>() : riddlesItems(entries);
                maybeRender();
            }

            @Override
            public void onFailure(@NonNull retrofit2.Call<ApiEnvelope<List<HistoryEntryDto>>> call,
                                  @NonNull Throwable t) {
                maybeRender();
            }
        });
    }

    // ------------------------------------------------------------------
    // Heraheza (proverbs)
    // ------------------------------------------------------------------

    private void loadProverbs() {
        RinjoraProverbRepository pRepo = new RinjoraProverbRepository(this);
        api.proverbsHistoryStats().enqueue(new retrofit2.Callback<ApiEnvelope<ProverbHistoryStatsDto>>() {
            @Override
            public void onResponse(@NonNull retrofit2.Call<ApiEnvelope<ProverbHistoryStatsDto>> call,
                                   @NonNull retrofit2.Response<ApiEnvelope<ProverbHistoryStatsDto>> response) {
                ApiEnvelope<ProverbHistoryStatsDto> envelope = response.body();
                if (response.isSuccessful() && envelope != null && envelope.isSuccess()
                        && envelope.getData() != null) {
                    result.proverbStats = statsLine(envelope.getData().getTotalAttempts(),
                            envelope.getData().getSolved(), envelope.getData().getAccuracy());
                } else if (response.code() == 401) {
                    goToAuth();
                    return;
                } else {
                    result.proverbStats = "local: " + pRepo.countLocalSolved() + " solved";
                }
                maybeRender();
            }

            @Override
            public void onFailure(@NonNull retrofit2.Call<ApiEnvelope<ProverbHistoryStatsDto>> call,
                                  @NonNull Throwable t) {
                result.proverbStats = "local: " + pRepo.countLocalSolved() + " solved";
                maybeRender();
            }
        });

        api.proverbsHistory(15).enqueue(new retrofit2.Callback<ApiEnvelope<List<ProverbHistoryEntryDto>>>() {
            @Override
            public void onResponse(@NonNull retrofit2.Call<ApiEnvelope<List<ProverbHistoryEntryDto>>> call,
                                   @NonNull retrofit2.Response<ApiEnvelope<List<ProverbHistoryEntryDto>>> response) {
                List<ProverbHistoryEntryDto> entries = response.isSuccessful()
                        && response.body() != null ? response.body().getData() : null;
                result.proverbs = entries == null
                        ? new ArrayList<DisplayItem>() : proverbItems(entries);
                maybeRender();
            }

            @Override
            public void onFailure(@NonNull retrofit2.Call<ApiEnvelope<List<ProverbHistoryEntryDto>>> call,
                                  @NonNull Throwable t) {
                maybeRender();
            }
        });
    }

    // ------------------------------------------------------------------
    // Tujajure (jokes)
    // ------------------------------------------------------------------

    private void loadJokes() {
        RinjoraJokeRepository jRepo = new RinjoraJokeRepository(this);
        api.jokesHistoryStats().enqueue(new retrofit2.Callback<ApiEnvelope<JokeHistoryStatsDto>>() {
            @Override
            public void onResponse(@NonNull retrofit2.Call<ApiEnvelope<JokeHistoryStatsDto>> call,
                                   @NonNull retrofit2.Response<ApiEnvelope<JokeHistoryStatsDto>> response) {
                ApiEnvelope<JokeHistoryStatsDto> envelope = response.body();
                if (response.isSuccessful() && envelope != null && envelope.isSuccess()
                        && envelope.getData() != null) {
                    result.jokeStats = statsLine(envelope.getData().getTotalAttempts(),
                            envelope.getData().getSolved(), envelope.getData().getAccuracy());
                } else if (response.code() == 401) {
                    goToAuth();
                    return;
                } else {
                    result.jokeStats = "local: " + jRepo.countLocalSolved() + " solved";
                }
                maybeRender();
            }

            @Override
            public void onFailure(@NonNull retrofit2.Call<ApiEnvelope<JokeHistoryStatsDto>> call,
                                  @NonNull Throwable t) {
                result.jokeStats = "local: " + jRepo.countLocalSolved() + " solved";
                maybeRender();
            }
        });

        api.jokesHistory(15).enqueue(new retrofit2.Callback<ApiEnvelope<List<JokeHistoryEntryDto>>>() {
            @Override
            public void onResponse(@NonNull retrofit2.Call<ApiEnvelope<List<JokeHistoryEntryDto>>> call,
                                   @NonNull retrofit2.Response<ApiEnvelope<List<JokeHistoryEntryDto>>> response) {
                List<JokeHistoryEntryDto> entries = response.isSuccessful()
                        && response.body() != null ? response.body().getData() : null;
                result.jokes = entries == null
                        ? new ArrayList<DisplayItem>() : jokeItems(entries);
                maybeRender();
            }

            @Override
            public void onFailure(@NonNull retrofit2.Call<ApiEnvelope<List<JokeHistoryEntryDto>>> call,
                                  @NonNull Throwable t) {
                maybeRender();
            }
        });
    }

    // ------------------------------------------------------------------
    // Build & render
    // ------------------------------------------------------------------

    private synchronized void maybeRender() {
        if (rendered) return;
        pendingCalls--;
        if (pendingCalls > 0) return;
        rendered = true;

        List<DisplayItem> all = new ArrayList<>();
        all.add(new DisplayItem("Sokwe · Riddles", result.riddleStats));
        all.addAll(result.riddles.isEmpty() ? empty("No riddle attempts yet.") : result.riddles);
        all.add(new DisplayItem("Heraheza · Proverbs", result.proverbStats));
        all.addAll(result.proverbs.isEmpty() ? empty("No proverb attempts yet.") : result.proverbs);
        all.add(new DisplayItem("Tujajure · Jokes", result.jokeStats));
        all.addAll(result.jokes.isEmpty() ? empty("No joke attempts yet.") : result.jokes);

        binding.progressBar.setVisibility(View.GONE);
        binding.swipeRefresh.setRefreshing(false);
        binding.tvStats.setText("" + 3 + " game modes");

        boolean hasAny = !result.riddles.isEmpty() || !result.proverbs.isEmpty() || !result.jokes.isEmpty();
        if (!hasAny) {
            binding.tvEmpty.setText("No attempts yet. Play a riddle, proverb or joke and it will show up here.");
            binding.tvEmpty.setVisibility(View.VISIBLE);
            binding.recyclerHistory.setVisibility(View.GONE);
        } else {
            binding.tvEmpty.setVisibility(View.GONE);
            binding.recyclerHistory.setVisibility(View.VISIBLE);
            adapter.submit(all);
        }
    }

    private List<DisplayItem> empty(String title) {
        List<DisplayItem> out = new ArrayList<>();
        out.add(new DisplayItem(title, "—", 0));
        return out;
    }

    private List<DisplayItem> riddlesItems(List<HistoryEntryDto> entries) {
        List<DisplayItem> out = new ArrayList<>();
        for (HistoryEntryDto e : entries) {
            String q = e.getRiddle() != null
                    ? e.getRiddle().getQuestion()
                    : ("Riddle #" + (e.getRiddle() != null ? e.getRiddle().getId() : e.getId()));
            out.add(new DisplayItem(q, detail(e.getSubmittedAnswer(), e.getAttemptedAt(),
                    e.isCorrect(), e.isRewarded()), dotColor(e.isCorrect())));
        }
        return out;
    }

    private List<DisplayItem> proverbItems(List<ProverbHistoryEntryDto> entries) {
        List<DisplayItem> out = new ArrayList<>();
        for (ProverbHistoryEntryDto e : entries) {
            String q = e.getProverb() != null
                    ? e.getProverb().getQuestion()
                    : ("Proverb #" + (e.getProverb() != null ? e.getProverb().getId() : e.getId()));
            out.add(new DisplayItem(q, detail(e.getSubmittedAnswer(), e.getAttemptedAt(),
                    e.isCorrect(), e.isRewarded()), dotColor(e.isCorrect())));
        }
        return out;
    }

    private List<DisplayItem> jokeItems(List<JokeHistoryEntryDto> entries) {
        List<DisplayItem> out = new ArrayList<>();
        for (JokeHistoryEntryDto e : entries) {
            String q = e.getJoke() != null && e.getJoke().getSetup() != null
                    ? e.getJoke().getSetup()
                    : ("Joke #" + e.getId());
            out.add(new DisplayItem(q, detail(e.getChosenOption(), e.getAttemptedAt(),
                    e.isCorrect(), e.isRewarded()), dotColor(e.isCorrect())));
        }
        return out;
    }

    private String detail(String submitted, String attemptedAt, boolean correct, boolean rewarded) {
        StringBuilder sb = new StringBuilder();
        sb.append("Your answer: “").append(submitted == null ? "" : submitted).append("”");
        java.util.Locale loc = java.util.Locale.getDefault();
        sb.append(String.format(loc, "  ·  %tD", parseDate(attemptedAt)));
        sb.append(correct ? "  ·  Correct" : "  ·  Incorrect");
        if (correct && rewarded) sb.append(" (+points)");
        return sb.toString();
    }

    private String statsLine(long attempts, long solved, double accuracy) {
        java.util.Locale loc = java.util.Locale.getDefault();
        return String.format(loc, "%d attempts · %d solved · %d%% accurate",
                attempts, solved, (int) Math.round(accuracy));
    }

    private int dotColor(boolean correct) {
        return ContextCompat.getColor(this,
                correct ? R.color.brand_success : R.color.brand_error);
    }

    private class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.VH> {
        private final List<DisplayItem> items = new ArrayList<>();

        void clear() {
            items.clear();
            notifyDataSetChanged();
        }

        void submit(List<DisplayItem> items) {
            this.items.clear();
            this.items.addAll(items);
            notifyDataSetChanged();
        }

        @Override
        public int getItemViewType(int position) {
            return items.get(position).type;
        }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            if (viewType == TYPE_HEADER) {
                ItemRinjoraHistoryHeaderBinding b = ItemRinjoraHistoryHeaderBinding.inflate(
                        LayoutInflater.from(parent.getContext()), parent, false);
                return new HeaderVH(b);
            }
            ItemRinjoraHistoryBinding b = ItemRinjoraHistoryBinding.inflate(
                    LayoutInflater.from(parent.getContext()), parent, false);
            return new EntryVH(b);
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            DisplayItem it = items.get(position);
            if (holder instanceof HeaderVH) {
                HeaderVH h = (HeaderVH) holder;
                h.binding.tvSectionTitle.setText(it.title);
                h.binding.tvSectionStats.setText(it.stats);
                h.binding.ivSectionDot.setBackgroundColor(ContextCompat.getColor(
                        RinjoraHistoryActivity.this, R.color.brand_secondary));
            } else {
                EntryVH e = (EntryVH) holder;
                e.binding.tvQuestion.setText(it.title);
                e.binding.tvDetail.setText(it.detail);
                e.binding.ivMarker.setBackgroundColor(it.dotColor);
            }
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        abstract class VH extends RecyclerView.ViewHolder {
            VH(@NonNull View itemView) {
                super(itemView);
            }
        }

        class HeaderVH extends VH {
            final ItemRinjoraHistoryHeaderBinding binding;

            HeaderVH(ItemRinjoraHistoryHeaderBinding binding) {
                super(binding.getRoot());
                this.binding = binding;
            }
        }

        class EntryVH extends VH {
            final ItemRinjoraHistoryBinding binding;

            EntryVH(ItemRinjoraHistoryBinding binding) {
                super(binding.getRoot());
                this.binding = binding;
            }
        }
    }

    private java.util.Date parseDate(String attemptedAt) {
        if (attemptedAt == null) return new java.util.Date();
        String s = attemptedAt;
        int t = s.indexOf('T');
        if (t > 0 && s.endsWith("Z")) s = s.substring(0, t) + " " + s.substring(t + 1, s.length() - 1);
        try {
            return new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.US).parse(s);
        } catch (Exception e) {
            try {
                return new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.US)
                        .parse(attemptedAt.replace("Z", ""));
            } catch (Exception e2) {
                return new java.util.Date();
            }
        }
    }

    private void goToAuth() {
        Intent intent = new Intent(this, RinjoraAuthActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
