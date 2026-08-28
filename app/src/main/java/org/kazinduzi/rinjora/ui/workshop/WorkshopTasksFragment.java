package org.kazinduzi.rinjora.ui.workshop;

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
import com.android.volley.toolbox.JsonArrayRequest;

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
import org.kazinduzi.rinjora.databinding.FragmentWorkshopTasksBinding;
import org.kazinduzi.rinjora.entities.User;
import org.kazinduzi.rinjora.entities.User_;
import org.kazinduzi.rinjora.models.RepairTask;
import org.kazinduzi.rinjora.network.ApiConfig;
import org.kazinduzi.rinjora.network.VolleySingleton;

public class WorkshopTasksFragment extends Fragment {

    private FragmentWorkshopTasksBinding binding;
    private VolleySingleton volley;
    private String token;
    private WorkshopTaskAdapter adapter;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentWorkshopTasksBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        volley = VolleySingleton.getInstance(requireContext());
        token = getAuthToken();

        adapter = new WorkshopTaskAdapter();
        binding.rvTasks.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvTasks.setAdapter(adapter);

        adapter.setOnTaskClickListener(task -> {
            Intent intent = new Intent(getContext(), WorkshopTaskDetailActivity.class);
            intent.putExtra("assignment_id", task.getId());
            startActivity(intent);
        });

        binding.btnRetry.setOnClickListener(v -> loadTasks());

        loadTasks();
    }

    private void loadTasks() {
        binding.progressBar.setVisibility(View.VISIBLE);
        binding.emptyState.setVisibility(View.GONE);
        binding.rvTasks.setVisibility(View.GONE);

        JsonArrayRequest request = new JsonArrayRequest(
                Request.Method.GET, ApiConfig.WORKSHOP_MY_TASKS, null,
                response -> {
                    binding.progressBar.setVisibility(View.GONE);
                    List<RepairTask> tasks = parseTasks(response);
                    if (tasks.isEmpty()) {
                        binding.emptyState.setVisibility(View.VISIBLE);
                        binding.tvEmpty.setText("No tasks assigned");
                        binding.btnRetry.setVisibility(View.GONE);
                    } else {
                        tasks.sort((a, b) -> statusRank(a.getStatus()) - statusRank(b.getStatus()));
                        adapter.setTasks(tasks);
                        binding.rvTasks.setVisibility(View.VISIBLE);
                    }
                },
                error -> {
                    binding.progressBar.setVisibility(View.GONE);
                    binding.emptyState.setVisibility(View.VISIBLE);
                    binding.tvEmpty.setText("Failed to load tasks");
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

    private int statusRank(String status) {
        if (status == null) return 1;
        switch (status.toLowerCase(java.util.Locale.ROOT)) {
            case "assigned": return 0;
            case "in_progress": return 1;
            case "completed": return 2;
            default: return 1;
        }
    }

    private List<RepairTask> parseTasks(JSONArray arr) {
        List<RepairTask> list = new ArrayList<>();
        for (int i = 0; i < arr.length(); i++) {
            try {
                JSONObject j = arr.getJSONObject(i);
                RepairTask t = new RepairTask();
                t.setId(j.optInt("id"));
                t.setRepairRequestId(j.optInt("repair_request_id"));
                t.setReference(j.optString("reference"));
                t.setVehiclePlate(j.optString("vehicle_plate"));
                t.setInstructions(j.optString("instructions"));
                t.setStatus(j.optString("status"));
                t.setDuration(j.isNull("duration") ? null : j.optString("duration"));
                t.setAssignedAt(j.optString("assigned_at"));
                t.setStartedAt(j.isNull("started_at") ? null : j.optString("started_at"));
                t.setCompletedAt(j.isNull("completed_at") ? null : j.optString("completed_at"));
                t.setCompletedNote(j.isNull("completed_note") ? null : j.optString("completed_note"));
                list.add(t);
            } catch (JSONException ignored) {}
        }
        return list;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (binding != null) loadTasks();
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
