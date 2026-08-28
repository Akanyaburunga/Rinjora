package org.kazinduzi.rinjora.ui.workshop;

import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import com.android.volley.Request;
import com.android.volley.toolbox.JsonObjectRequest;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import io.objectbox.Box;
import org.kazinduzi.rinjora.BaseActivity;
import org.kazinduzi.rinjora.MyApp;
import org.kazinduzi.rinjora.R;
import org.kazinduzi.rinjora.databinding.ActivityWorkshopTaskDetailBinding;
import org.kazinduzi.rinjora.util.DateUtils;
import org.kazinduzi.rinjora.util.AnalyticsHelper;
import org.kazinduzi.rinjora.entities.User;
import org.kazinduzi.rinjora.entities.User_;
import org.kazinduzi.rinjora.models.RepairItem;
import org.kazinduzi.rinjora.models.RepairRequest;
import org.kazinduzi.rinjora.models.RepairTask;
import org.kazinduzi.rinjora.models.RepairTaskDetail;
import org.kazinduzi.rinjora.network.ApiConfig;
import org.kazinduzi.rinjora.network.VolleySingleton;

public class WorkshopTaskDetailActivity extends BaseActivity {

    private ActivityWorkshopTaskDetailBinding binding;
    private VolleySingleton volley;
    private String token;
    private int assignmentId;
    private RepairTaskDetail current;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityWorkshopTaskDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        assignmentId = getIntent().getIntExtra("assignment_id", 0);
        volley = VolleySingleton.getInstance(this);
        token = getAuthToken();

        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        binding.toolbar.setNavigationOnClickListener(v -> finish());

        binding.btnStart.setOnClickListener(v -> startWork());
        binding.btnComplete.setOnClickListener(v -> promptComplete());

        loadDetail();
    }

    private void loadDetail() {
        binding.progressBar.setVisibility(View.VISIBLE);
        binding.scrollView.setVisibility(View.GONE);

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.GET, ApiConfig.workshopTaskDetail(assignmentId), null,
                response -> {
                    binding.progressBar.setVisibility(View.GONE);
                    current = parseDetail(response);
                    bindDetail();
                },
                error -> {
                    binding.progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Failed to load task", Toast.LENGTH_SHORT).show();
                    finish();
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

    private void bindDetail() {
        if (current == null) return;

        RepairTask a = current.getAssignment();
        RepairRequest rr = current.getRepairRequest();

        String ref = a.getReference() != null ? a.getReference() : "#" + a.getId();
        binding.tvReference.setText(ref);
        getSupportActionBar().setTitle(ref);

        String status = a.getStatus() != null ? a.getStatus() : "";
        binding.tvStatus.setText(status.toUpperCase(Locale.ROOT).replace('_', ' '));
        int statusColor = ContextCompat.getColor(this, statusColor(status));
        binding.tvStatus.setTextColor(statusColor);
        binding.tvStatus.setBackgroundTintList(android.content.res.ColorStateList.valueOf(statusColor & 0x1FFFFFFF | 0x22000000));

        binding.tvVehicle.setText(rr != null && rr.getVehicle() != null ? rr.getVehicle().getPlateNumber() : "");
        binding.tvInstructions.setText(a.getInstructions());
        binding.tvDuration.setText(a.getDuration() != null ? "Duration: " + a.getDuration() : "");

        StringBuilder dates = new StringBuilder();
        if (a.getAssignedAt() != null) dates.append("Assigned: ").append(DateUtils.dateTime(a.getAssignedAt()));
        if (a.getStartedAt() != null) dates.append("\nStarted: ").append(DateUtils.dateTime(a.getStartedAt()));
        if (a.getCompletedAt() != null) dates.append("\nCompleted: ").append(DateUtils.dateTime(a.getCompletedAt()));
        binding.tvDates.setText(dates.toString().trim());

        if (a.getCompletedNote() != null && !a.getCompletedNote().isEmpty()) {
            binding.tvCompletedNote.setVisibility(View.VISIBLE);
            binding.tvCompletedNote.setText("Note: " + a.getCompletedNote());
        }

        StringBuilder driver = new StringBuilder();
        if (rr != null && rr.getDriver() != null) {
            driver.append("Driver: ").append(rr.getDriver().getName());
            if (rr.getDriver().getPhone() != null) driver.append("  ").append(rr.getDriver().getPhone());
        }
        binding.tvDriver.setText(driver.toString().trim());
        if (binding.tvDriver.getText().length() == 0) {
            binding.cardDriver.setVisibility(View.GONE);
        }

        if (rr != null) {
            StringBuilder tp = new StringBuilder();
            if (rr.getType() != null) tp.append(rr.getType());
            if (rr.getPriority() != null) tp.append("  â€¢  ").append(rr.getPriority().toUpperCase(Locale.ROOT));
            binding.tvTypePriority.setText(tp.toString().trim());
            binding.tvDescription.setText(rr.getDescription());
            buildItems(rr);
        }

        boolean assigned = status.equalsIgnoreCase("assigned");
        boolean inProgress = status.equalsIgnoreCase("in_progress");
        binding.btnStart.setVisibility(assigned ? View.VISIBLE : View.GONE);
        binding.btnComplete.setVisibility(inProgress ? View.VISIBLE : View.GONE);

        binding.scrollView.setVisibility(View.VISIBLE);
    }

    private void buildItems(RepairRequest rr) {
        binding.itemsContainer.removeAllViews();
        if (rr.getItems() == null || rr.getItems().isEmpty()) {
            binding.cardItems.setVisibility(View.GONE);
            return;
        }
        binding.cardItems.setVisibility(View.VISIBLE);
        for (RepairItem item : rr.getItems()) {
            StringBuilder sb = new StringBuilder();
            if (item.getDescription() != null) sb.append(item.getDescription());
            if (item.getPart() != null && item.getPart().getName() != null) {
                sb.append("\nPart: ").append(item.getPart().getName());
                if (item.getPart().getSku() != null) sb.append(" (").append(item.getPart().getSku()).append(")");
            }
            sb.append(String.format(Locale.ROOT, "\nEst: %.1f x %.2f = %.2f",
                    item.getEstimatedQuantity(), item.getEstimatedUnitPrice(), item.getEstimatedTotal()));
            addItemRow(sb.toString().trim());
        }
    }

    private void addItemRow(String text) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextSize(14);
        tv.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
        tv.setLineSpacing(0, 1.1f);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = 12;
        binding.itemsContainer.addView(tv, lp);
    }

    private void startWork() {
        binding.btnStart.setEnabled(false);
        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.POST, ApiConfig.workshopTaskStart(assignmentId), null,
                response -> {
                    Toast.makeText(this, "Work started", Toast.LENGTH_SHORT).show();
                    AnalyticsHelper.logEvent(this, "workshop_task_started");
                    loadDetail();
                },
                error -> {
                    binding.btnStart.setEnabled(true);
                    Toast.makeText(this, errorMessage(error, "Failed to start work"), Toast.LENGTH_SHORT).show();
                    loadDetail();
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

    private void promptComplete() {
        com.google.android.material.dialog.MaterialAlertDialogBuilder builder =
                new com.google.android.material.dialog.MaterialAlertDialogBuilder(this);
        builder.setTitle("Complete Work");

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(48, 24, 48, 24);

        com.google.android.material.textfield.TextInputEditText note =
                new com.google.android.material.textfield.TextInputEditText(this);
        note.setHint("Completed note (optional)");
        note.setMinLines(3);
        note.setGravity(android.view.Gravity.TOP);
        layout.addView(note);

        builder.setView(layout);
        builder.setPositiveButton("Complete", (d, w) -> {
            String text = note.getText() != null ? note.getText().toString().trim() : "";
            completeWork(text);
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void completeWork(String completedNote) {
        binding.btnComplete.setEnabled(false);
        JSONObject body = new JSONObject();
        try {
            if (!completedNote.isEmpty()) body.put("completed_note", completedNote);
        } catch (JSONException ignored) {}

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.POST, ApiConfig.workshopTaskComplete(assignmentId), body,
                response -> {
                    Toast.makeText(this, "Work completed", Toast.LENGTH_SHORT).show();
                    AnalyticsHelper.logEvent(this, "workshop_task_completed");
                    loadDetail();
                },
                error -> {
                    binding.btnComplete.setEnabled(true);
                    Toast.makeText(this, errorMessage(error, "Failed to complete work"), Toast.LENGTH_SHORT).show();
                    loadDetail();
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

    private String errorMessage(com.android.volley.VolleyError error, String fallback) {
        if (error.networkResponse != null && error.networkResponse.data != null) {
            try {
                String json = new String(error.networkResponse.data, "UTF-8");
                JSONObject err = new JSONObject(json);
                if (err.has("message")) return err.getString("message");
            } catch (Exception ignored) {}
        }
        return fallback;
    }

    private RepairTaskDetail parseDetail(JSONObject response) {
        try {
            JSONObject data = response.optJSONObject("data");
            if (data == null) data = response;

            RepairTaskDetail detail = new RepairTaskDetail();

            JSONObject a = data.getJSONObject("assignment");
            RepairTask task = new RepairTask();
            task.setId(a.optInt("id"));
            task.setReference(a.optString("reference"));
            task.setInstructions(a.optString("instructions"));
            task.setStatus(a.optString("status"));
            task.setDuration(a.isNull("duration") ? null : a.optString("duration"));
            task.setAssignedAt(a.optString("assigned_at"));
            task.setStartedAt(a.isNull("started_at") ? null : a.optString("started_at"));
            task.setCompletedAt(a.isNull("completed_at") ? null : a.optString("completed_at"));
            task.setCompletedNote(a.isNull("completed_note") ? null : a.optString("completed_note"));
            detail.setAssignment(task);

            JSONObject rr = data.getJSONObject("repair_request");
            RepairRequest request = new RepairRequest();
            request.setReference(rr.optString("reference"));
            request.setType(rr.optString("type"));
            request.setPriority(rr.optString("priority"));
            request.setDescription(rr.optString("description"));
            request.setStatus(rr.optString("status"));

            if (rr.has("vehicle") && !rr.isNull("vehicle")) {
                JSONObject v = rr.getJSONObject("vehicle");
                RepairRequest.Vehicle vehicle = new RepairRequest.Vehicle();
                vehicle.setId(v.optInt("id"));
                vehicle.setPlateNumber(v.optString("plate_number"));
                vehicle.setStatus(v.optString("status"));
                request.setVehicle(vehicle);
            }

            if (rr.has("driver") && !rr.isNull("driver")) {
                JSONObject d = rr.getJSONObject("driver");
                RepairRequest.Driver driver = new RepairRequest.Driver();
                driver.setId(d.optInt("id"));
                driver.setName(d.optString("name"));
                driver.setPhone(d.optString("phone"));
                request.setDriver(driver);
            }

            java.util.List<RepairItem> items = new java.util.ArrayList<>();
            if (rr.has("items")) {
                JSONArray arr = rr.getJSONArray("items");
                for (int i = 0; i < arr.length(); i++) {
                    JSONObject j = arr.getJSONObject(i);
                    RepairItem item = new RepairItem();
                    item.setDescription(j.optString("description"));
                    item.setEstimatedQuantity(j.optDouble("estimated_quantity", 0));
                    item.setEstimatedUnitPrice(j.optDouble("estimated_unit_price", 0));
                    item.setEstimatedTotal(j.optDouble("estimated_total", 0));
                    if (j.has("part") && !j.isNull("part")) {
                        JSONObject p = j.getJSONObject("part");
                        RepairItem.Part part = new RepairItem.Part();
                        part.setId(p.optInt("id"));
                        part.setName(p.optString("name"));
                        part.setSku(p.optString("sku"));
                        item.setPart(part);
                    }
                    items.add(item);
                }
            }
            request.setItems(items);
            detail.setRepairRequest(request);
            return detail;
        } catch (JSONException e) {
            return null;
        }
    }

    private int statusColor(String status) {
        if (status == null) return R.color.task_status_assigned;
        switch (status.toLowerCase(Locale.ROOT)) {
            case "assigned": return R.color.task_status_assigned;
            case "in_progress": return R.color.task_status_in_progress;
            case "completed": return R.color.task_status_completed;
            default: return R.color.task_status_assigned;
        }
    }

    private String getAuthToken() {
        try {
            Box<User> box = ((MyApp) getApplication()).getBoxStore().boxFor(User.class);
            User u = box.query().equal(User_.isActive, true).build().findFirst();
            return u != null ? u.getToken() : null;
        } catch (Exception e) {
            return null;
        }
    }
}
