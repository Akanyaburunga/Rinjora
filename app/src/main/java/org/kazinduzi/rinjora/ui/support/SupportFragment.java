package org.kazinduzi.rinjora.ui.support;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.android.volley.Request;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.JsonObjectRequest;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.objectbox.Box;
import org.kazinduzi.rinjora.MyApp;
import org.kazinduzi.rinjora.R;
import org.kazinduzi.rinjora.databinding.FragmentSupportBinding;
import org.kazinduzi.rinjora.util.AnalyticsHelper;
import org.kazinduzi.rinjora.entities.User;
import org.kazinduzi.rinjora.entities.User_;
import org.kazinduzi.rinjora.models.Ticket;
import org.kazinduzi.rinjora.network.ApiConfig;
import org.kazinduzi.rinjora.network.VolleySingleton;

public class SupportFragment extends Fragment {

    private FragmentSupportBinding binding;
    private SupportTicketAdapter adapter;
    private VolleySingleton volley;
    private String token;

    private List<Ticket.Category> categories = new ArrayList<>();
    private static final String[] PRIORITIES = {"low", "normal", "high", "urgent"};

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentSupportBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        volley = VolleySingleton.getInstance(requireContext());
        token = getAuthToken();

        adapter = new SupportTicketAdapter();
        binding.rvTickets.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvTickets.setAdapter(adapter);

        adapter.setOnTicketClickListener(ticket -> {
            Intent intent = new Intent(getContext(), TicketDetailActivity.class);
            intent.putExtra("ticket_id", ticket.getId());
            startActivity(intent);
        });

        binding.fabCreateTicket.setOnClickListener(v -> showCreateDialog());
        binding.btnCreateFromEmpty.setOnClickListener(v -> showCreateDialog());
        binding.btnCreateHeader.setOnClickListener(v -> showCreateDialog());
        binding.btnRetry.setOnClickListener(v -> loadTickets());

        loadCategories();
    }

    private void loadCategories() {
        JsonArrayRequest request = new JsonArrayRequest(
                Request.Method.GET, ApiConfig.SUPPORT_CATEGORIES, null,
                response -> {
                    categories = parseCategories(response);
                },
                error -> {
                    categories = new ArrayList<>();
                }
        ) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> h = new HashMap<>();
                h.put("Accept", "application/json");
                if (token != null) h.put("Authorization", "Bearer " + token);
                return h;
            }
        };
        volley.addToRequestQueue(request);
    }

    private List<Ticket.Category> parseCategories(JSONArray arr) {
        List<Ticket.Category> list = new ArrayList<>();
        for (int i = 0; i < arr.length(); i++) {
            try {
                JSONObject j = arr.getJSONObject(i);
                Ticket.Category c = new Ticket.Category();
                c.setId(j.optInt("id"));
                c.setName(j.optString("name"));
                list.add(c);
            } catch (JSONException ignored) {}
        }
        return list;
    }

    private void loadTickets() {
        binding.progressBar.setVisibility(View.VISIBLE);
        binding.emptyState.setVisibility(View.GONE);
        binding.btnCreateHeader.setVisibility(View.GONE);
        binding.rvTickets.setVisibility(View.GONE);

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.GET, ApiConfig.SUPPORT_TICKETS, null,
                response -> {
                    binding.progressBar.setVisibility(View.GONE);
                    List<Ticket> tickets = parseTickets(response);
                    if (tickets.isEmpty()) {
                        binding.emptyState.setVisibility(View.VISIBLE);
                        binding.tvEmpty.setText("No support tickets yet");
                        binding.btnCreateFromEmpty.setVisibility(View.VISIBLE);
                        binding.btnRetry.setVisibility(View.GONE);
                    } else {
                        adapter.setTickets(tickets);
                        binding.btnCreateHeader.setVisibility(View.VISIBLE);
                        binding.rvTickets.setVisibility(View.VISIBLE);
                    }
                },
                error -> {
                    binding.progressBar.setVisibility(View.GONE);
                    binding.emptyState.setVisibility(View.VISIBLE);
                    binding.tvEmpty.setText("Failed to load tickets");
                    binding.btnCreateFromEmpty.setVisibility(View.GONE);
                    binding.btnRetry.setVisibility(View.VISIBLE);
                    Toast.makeText(getContext(), "Network error", Toast.LENGTH_SHORT).show();
                }
        ) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> h = new HashMap<>();
                h.put("Accept", "application/json");
                if (token != null) h.put("Authorization", "Bearer " + token);
                return h;
            }
        };
        volley.addToRequestQueue(request);
    }

    private void showCreateDialog() {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(requireContext());
        builder.setTitle("New Support Ticket");

        LinearLayout layout = new LinearLayout(requireContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(48, 24, 48, 24);

        com.google.android.material.textfield.TextInputEditText etTitle = new com.google.android.material.textfield.TextInputEditText(requireContext());
        etTitle.setHint("Title *");
        etTitle.setInputType(android.text.InputType.TYPE_CLASS_TEXT);
        layout.addView(etTitle);

        addSpacer(layout);

        com.google.android.material.textfield.TextInputEditText etDescription = new com.google.android.material.textfield.TextInputEditText(requireContext());
        etDescription.setHint("Description *");
        etDescription.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        etDescription.setMinLines(3);
        layout.addView(etDescription);

        addSpacer(layout);

        android.widget.TextView tvCatLabel = new android.widget.TextView(requireContext());
        tvCatLabel.setText("Category *");
        tvCatLabel.setTextSize(14);
        tvCatLabel.setTextColor(androidx.core.content.ContextCompat.getColor(requireContext(), R.color.text_secondary));
        layout.addView(tvCatLabel);

        List<String> catNames = new ArrayList<>();
        for (Ticket.Category c : categories) catNames.add(c.getName());

        android.widget.Spinner spinnerCategory = new android.widget.Spinner(requireContext());
        ArrayAdapter<String> catAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_dropdown_item, catNames);
        spinnerCategory.setAdapter(catAdapter);
        layout.addView(spinnerCategory);

        addSpacer(layout);

        android.widget.TextView tvPriLabel = new android.widget.TextView(requireContext());
        tvPriLabel.setText("Priority *");
        tvPriLabel.setTextSize(14);
        tvPriLabel.setTextColor(androidx.core.content.ContextCompat.getColor(requireContext(), R.color.text_secondary));
        layout.addView(tvPriLabel);

        android.widget.Spinner spinnerPriority = new android.widget.Spinner(requireContext());
        ArrayAdapter<String> priAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_dropdown_item, PRIORITIES);
        spinnerPriority.setAdapter(priAdapter);
        layout.addView(spinnerPriority);

        addSpacer(layout);

        com.google.android.material.textfield.TextInputEditText etSubjectType = new com.google.android.material.textfield.TextInputEditText(requireContext());
        etSubjectType.setHint("Subject type (optional)");
        etSubjectType.setInputType(android.text.InputType.TYPE_CLASS_TEXT);
        layout.addView(etSubjectType);

        addSpacer(layout);

        com.google.android.material.textfield.TextInputEditText etSubjectId = new com.google.android.material.textfield.TextInputEditText(requireContext());
        etSubjectId.setHint("Subject ID (optional)");
        etSubjectId.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        layout.addView(etSubjectId);

        builder.setView(layout);
        builder.setPositiveButton("Submit", (dialog, which) -> {
            String title = etTitle.getText().toString().trim();
            String description = etDescription.getText().toString().trim();
            String subjectType = etSubjectType.getText().toString().trim();
            String subjectIdStr = etSubjectId.getText().toString().trim();

            if (title.isEmpty()) {
                Toast.makeText(getContext(), "Title is required", Toast.LENGTH_SHORT).show();
                return;
            }
            if (description.isEmpty()) {
                Toast.makeText(getContext(), "Description is required", Toast.LENGTH_SHORT).show();
                return;
            }
            if (categories.isEmpty()) {
                Toast.makeText(getContext(), "Categories not loaded yet", Toast.LENGTH_SHORT).show();
                return;
            }

            int catPos = spinnerCategory.getSelectedItemPosition();
            int catId = categories.get(catPos).getId();
            String priority = PRIORITIES[spinnerPriority.getSelectedItemPosition()];
            Integer subjectId = subjectIdStr.isEmpty() ? null : Integer.parseInt(subjectIdStr);

            createTicket(title, description, catId, priority, subjectType.isEmpty() ? null : subjectType, subjectId);
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void addSpacer(LinearLayout layout) {
        View spacer = new View(requireContext());
        spacer.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 16));
        layout.addView(spacer);
    }

    private void createTicket(String title, String description, int categoryId, String priority, String subjectType, Integer subjectId) {
        binding.progressBar.setVisibility(View.VISIBLE);

        JSONObject body = new JSONObject();
        try {
            body.put("title", title);
            body.put("description", description);
            body.put("support_category_id", categoryId);
            body.put("priority", priority);
            if (subjectType != null) body.put("subject_type", subjectType);
            if (subjectId != null) body.put("subject_id", subjectId);
        } catch (JSONException ignored) {}

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.POST, ApiConfig.SUPPORT_TICKETS, body,
                response -> {
                    Toast.makeText(getContext(), "Ticket created", Toast.LENGTH_SHORT).show();
                    if (getContext() != null) AnalyticsHelper.logEvent(getContext(), "support_ticket_created");
                    loadTickets();
                },
                error -> {
                    binding.progressBar.setVisibility(View.GONE);
                    String msg = "Failed to create ticket";
                    if (error.networkResponse != null && error.networkResponse.data != null) {
                        try {
                            String json = new String(error.networkResponse.data, "UTF-8");
                            JSONObject err = new JSONObject(json);
                            if (err.has("message")) msg = err.getString("message");
                            if (err.has("errors")) {
                                JSONObject errors = err.getJSONObject("errors");
                                StringBuilder sb = new StringBuilder();
                                for (int i = 0; i < errors.names().length(); i++) {
                                    String key = errors.names().getString(i);
                                    JSONArray arr = errors.getJSONArray(key);
                                    if (arr.length() > 0) sb.append(arr.optString(0)).append("\n");
                                }
                                if (sb.length() > 0) msg = sb.toString().trim();
                            }
                        } catch (Exception ignored) {}
                    }
                    Toast.makeText(getContext(), msg, Toast.LENGTH_SHORT).show();
                }
        ) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> h = new HashMap<>();
                h.put("Content-Type", "application/json");
                h.put("Accept", "application/json");
                if (token != null) h.put("Authorization", "Bearer " + token);
                return h;
            }
        };
        volley.addToRequestQueue(request);
    }

    private List<Ticket> parseTickets(JSONObject response) {
        List<Ticket> list = new ArrayList<>();
        try {
            JSONArray data = response.optJSONArray("data");
            if (data == null) return list;
            for (int i = 0; i < data.length(); i++) {
                JSONObject j = data.getJSONObject(i);
                Ticket t = new Ticket();
                t.setId(j.optInt("id"));
                t.setReference(j.optString("reference"));
                t.setTitle(j.optString("title"));
                t.setStatus(j.optString("status"));
                t.setPriority(j.optString("priority"));
                t.setCreatedAt(j.optString("created_at"));
                t.setDescription(j.optString("description"));
                list.add(t);
            }
        } catch (JSONException ignored) {}
        return list;
    }

    private String getAuthToken() {
        try {
            Box<User> box = ((MyApp) requireActivity().getApplication()).getBoxStore().boxFor(User.class);
            User u = box.query().equal(User_.isActive, true).build().findFirst();
            return u != null ? u.getToken() : null;
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        loadTickets();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
