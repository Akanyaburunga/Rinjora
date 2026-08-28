package org.kazinduzi.rinjora.ui.repairs;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.android.volley.Request;
import com.android.volley.toolbox.JsonObjectRequest;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import io.objectbox.Box;
import org.kazinduzi.rinjora.MyApp;
import org.kazinduzi.rinjora.R;
import org.kazinduzi.rinjora.databinding.FragmentRepairRequestsBinding;
import org.kazinduzi.rinjora.entities.User;
import org.kazinduzi.rinjora.entities.User_;
import org.kazinduzi.rinjora.models.RepairRequest;
import org.kazinduzi.rinjora.network.ApiConfig;
import org.kazinduzi.rinjora.network.VolleySingleton;

public class RepairRequestsFragment extends Fragment {

    private FragmentRepairRequestsBinding binding;
    private VolleySingleton volley;
    private String token;

    private final List<RepairRequest> allRequests = new ArrayList<>();
    private RepairRequestAdapter adapter;
    private int currentRequestId;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentRepairRequestsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        volley = VolleySingleton.getInstance(requireContext());
        token = getAuthToken();

        adapter = new RepairRequestAdapter();
        binding.rvRequests.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvRequests.setAdapter(adapter);

        adapter.setOnRepairClickListener(request -> {
            Intent intent = new Intent(getContext(), RepairRequestDetailActivity.class);
            intent.putExtra("request_id", request.getId());
            startActivity(intent);
        });

        binding.fabNewRequest.setOnClickListener(v ->
                startActivity(new Intent(getContext(), CreateRepairRequestActivity.class)));
        binding.bannerCurrent.setOnClickListener(v -> openCurrentRequest());
        binding.btnRetry.setOnClickListener(v -> loadRequests());
        binding.filterGroup.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) applyFilter();
        });

        loadRequests();
        loadCurrentBanner();
    }

    private void openCurrentRequest() {
        if (currentRequestId <= 0) return;
        Intent intent = new Intent(getContext(), RepairRequestDetailActivity.class);
        intent.putExtra("request_id", currentRequestId);
        startActivity(intent);
    }

    private void loadCurrentBanner() {
        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.GET, ApiConfig.REPAIR_CURRENT, null,
                response -> {
                    JSONObject data = response.optJSONObject("data");
                    if (data == null) data = response;
                    if (data.length() == 0) {
                        binding.bannerCurrent.setVisibility(View.GONE);
                        return;
                    }
                    currentRequestId = data.optInt("id");
                    StringBuilder subtitle = new StringBuilder();
                    String ref = data.optString("reference");
                    if (!ref.isEmpty()) subtitle.append(ref);
                    if (data.has("vehicle") && !data.isNull("vehicle")) {
                        JSONObject v = data.optJSONObject("vehicle");
                        String plate = v != null ? v.optString("plate_number") : "";
                        if (!plate.isEmpty()) {
                            if (subtitle.length() > 0) subtitle.append("  •  ");
                            subtitle.append(plate);
                        }
                    }
                    binding.tvBannerSubtitle.setText(subtitle.toString());
                    binding.bannerCurrent.setVisibility(View.VISIBLE);
                },
                error -> binding.bannerCurrent.setVisibility(View.GONE)
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

    private void loadRequests() {
        binding.progressBar.setVisibility(View.VISIBLE);
        binding.emptyState.setVisibility(View.GONE);
        binding.rvRequests.setVisibility(View.GONE);

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.GET, ApiConfig.REPAIR_REQUESTS, null,
                response -> {
                    binding.progressBar.setVisibility(View.GONE);
                    allRequests.clear();
                    JSONArray arr = response.optJSONArray("data");
                    allRequests.addAll(parseRequests(arr != null ? arr : new JSONArray()));
                    if (allRequests.isEmpty()) {
                        binding.emptyState.setVisibility(View.VISIBLE);
                        binding.btnRetry.setVisibility(View.GONE);
                    } else {
                        applyFilter();
                    }
                },
                error -> {
                    binding.progressBar.setVisibility(View.GONE);
                    binding.emptyState.setVisibility(View.VISIBLE);
                    binding.tvEmpty.setText("Failed to load repair requests");
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

    private void applyFilter() {
        List<RepairRequest> filtered = new ArrayList<>();
        for (RepairRequest r : allRequests) {
            if (matchesFilter(r.getStatus())) filtered.add(r);
        }
        if (filtered.isEmpty()) {
            binding.emptyState.setVisibility(View.VISIBLE);
            binding.tvEmpty.setText("No repair requests in this filter");
            binding.btnRetry.setVisibility(View.GONE);
            binding.rvRequests.setVisibility(View.GONE);
        } else {
            binding.emptyState.setVisibility(View.GONE);
            binding.rvRequests.setVisibility(View.VISIBLE);
            adapter.setRequests(filtered);
        }
    }

    private boolean matchesFilter(String status) {
        int checked = binding.filterGroup.getCheckedButtonId();
        if (status == null) return false;
        String s = status.toLowerCase(Locale.ROOT);
        if (checked == R.id.btn_filter_completed) {
            return s.equals("completed") || s.equals("released");
        } else if (checked == R.id.btn_filter_all) {
            return true;
        } else {
            return !s.equals("completed") && !s.equals("released") && !s.equals("cancelled");
        }
    }

    private List<RepairRequest> parseRequests(JSONArray arr) {
        List<RepairRequest> list = new ArrayList<>();
        for (int i = 0; i < arr.length(); i++) {
            try {
                JSONObject j = arr.getJSONObject(i);
                RepairRequest r = new RepairRequest();
                r.setId(j.optInt("id"));
                r.setReference(j.optString("reference"));
                r.setType(j.optString("type"));
                r.setPriority(j.optString("priority"));
                r.setStatus(j.optString("status"));
                r.setDescription(j.optString("description"));
                r.setCreatedAt(j.optString("created_at"));
                r.setSubmittedAt(j.isNull("submitted_at") ? null : j.optString("submitted_at"));

                if (j.has("vehicle") && !j.isNull("vehicle")) {
                    JSONObject v = j.getJSONObject("vehicle");
                    RepairRequest.Vehicle vehicle = new RepairRequest.Vehicle();
                    vehicle.setId(v.optInt("id"));
                    vehicle.setPlateNumber(v.optString("plate_number"));
                    vehicle.setStatus(v.optString("status"));
                    r.setVehicle(vehicle);
                }
                list.add(r);
            } catch (JSONException ignored) {}
        }
        return list;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (binding != null) loadRequests();
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
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
