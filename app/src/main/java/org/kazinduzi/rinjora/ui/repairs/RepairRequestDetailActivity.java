package org.kazinduzi.rinjora.ui.repairs;

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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import io.objectbox.Box;
import org.kazinduzi.rinjora.BaseActivity;
import org.kazinduzi.rinjora.MyApp;
import org.kazinduzi.rinjora.R;
import org.kazinduzi.rinjora.databinding.ActivityRepairRequestDetailBinding;
import org.kazinduzi.rinjora.util.DateUtils;
import org.kazinduzi.rinjora.util.AnalyticsHelper;
import org.kazinduzi.rinjora.entities.User;
import org.kazinduzi.rinjora.entities.User_;
import org.kazinduzi.rinjora.models.RepairItem;
import org.kazinduzi.rinjora.models.RepairRequest;
import org.kazinduzi.rinjora.network.ApiConfig;
import org.kazinduzi.rinjora.network.VolleySingleton;

public class RepairRequestDetailActivity extends BaseActivity {

    private ActivityRepairRequestDetailBinding binding;
    private VolleySingleton volley;
    private String token;
    private int requestId;
    private RepairRequest current;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRepairRequestDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        requestId = getIntent().getIntExtra("request_id", 0);
        volley = VolleySingleton.getInstance(this);
        token = getAuthToken();

        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        binding.toolbar.setNavigationOnClickListener(v -> finish());

        binding.btnSubmit.setOnClickListener(v -> submitRequest());
        binding.btnCancel.setOnClickListener(v -> confirmCancel());

        loadDetail();
    }

    private void loadDetail() {
        binding.progressBar.setVisibility(View.VISIBLE);
        binding.scrollView.setVisibility(View.GONE);

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.GET, ApiConfig.repairRequestDetail(requestId), null,
                response -> {
                    binding.progressBar.setVisibility(View.GONE);
                    JSONObject data = response.optJSONObject("data");
                    if (data == null) data = response;
                    current = parseRequest(data);
                    bindDetail();
                },
                error -> {
                    binding.progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Failed to load repair request", Toast.LENGTH_SHORT).show();
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

        String ref = current.getReference() != null ? current.getReference() : "#" + current.getId();
        binding.tvReference.setText(ref);
        getSupportActionBar().setTitle(ref);

        RepairRequest.Vehicle v = current.getVehicle();
        binding.tvVehicle.setText(v != null ? v.getPlateNumber() : "");

        StringBuilder tp = new StringBuilder();
        if (current.getType() != null) tp.append("Type: ").append(current.getType());
        if (current.getPriority() != null) tp.append("  •  Priority: ").append(current.getPriority());
        binding.tvTypePriority.setText(tp.toString().trim());

        binding.tvDescription.setText(current.getDescription());

        String status = current.getStatus() != null ? current.getStatus() : "";
        binding.tvStatus.setText(status.toUpperCase(Locale.ROOT).replace('_', ' '));
        int statusColor = ContextCompat.getColor(this, statusColor(status));
        binding.tvStatus.setTextColor(statusColor);
        binding.tvStatus.setBackgroundTintList(android.content.res.ColorStateList.valueOf(statusColor & 0x1FFFFFFF | 0x22000000));

        buildTimeline(status);
        buildItems();
        buildApprovals();
        buildAssignments();
        buildRelease();

        boolean draft = status.equalsIgnoreCase("draft");
        boolean cancellable = draft || status.equalsIgnoreCase("pending_approval")
                || status.equalsIgnoreCase("pending_ops_approval");
        binding.btnSubmit.setVisibility(draft ? View.VISIBLE : View.GONE);
        binding.btnCancel.setVisibility(cancellable ? View.VISIBLE : View.GONE);

        binding.scrollView.setVisibility(View.VISIBLE);
    }

    private void buildTimeline(String status) {
        int reached;
        switch (status.toLowerCase(Locale.ROOT)) {
            case "approved": reached = 2; break;
            case "in_progress": reached = 3; break;
            case "completed": reached = 4; break;
            case "released": reached = 5; break;
            case "pending_approval":
            case "pending_ops_approval": reached = 1; break;
            default: reached = 0;
        }

        String[] steps = {"Submitted", "Approved", "In Workshop", "Completed", "Released"};
        binding.timelineContainer.removeAllViews();
        for (int i = 0; i < steps.length; i++) {
            addTimelineRow(steps[i], i < reached, i == reached - 1);
        }
    }

    private void addTimelineRow(String label, boolean done, boolean active) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(android.view.Gravity.CENTER_VERTICAL);
        row.setPadding(0, 6, 0, 6);

        View dot = new View(this);
        int size = done || active ? 14 : 10;
        LinearLayout.LayoutParams dotLp = new LinearLayout.LayoutParams(size, size);
        dotLp.setMarginEnd(12);
        dot.setLayoutParams(dotLp);
        int dotColor;
        if (active) {
            dotColor = ContextCompat.getColor(this, R.color.brand_primary);
        } else if (done) {
            dotColor = ContextCompat.getColor(this, R.color.brand_success);
        } else {
            dotColor = ContextCompat.getColor(this, R.color.brand_outline);
        }
        dot.setBackground(new android.graphics.drawable.GradientDrawable() {{
            setShape(android.graphics.drawable.GradientDrawable.OVAL);
            setColor(dotColor);
        }});

        TextView tv = new TextView(this);
        tv.setText(label);
        tv.setTextSize(14);
        tv.setTextColor(ContextCompat.getColor(this,
                done || active ? R.color.text_primary : R.color.text_muted));
        tv.setTypeface(tv.getTypeface(), done || active ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL);

        row.addView(dot);
        row.addView(tv);
        binding.timelineContainer.addView(row);
    }

    private void buildItems() {
        binding.itemsContainer.removeAllViews();
        List<RepairItem> items = current.getItems();
        if (items == null || items.isEmpty()) {
            binding.cardItems.setVisibility(View.GONE);
            return;
        }
        binding.cardItems.setVisibility(View.VISIBLE);
        for (RepairItem item : items) {
            StringBuilder sb = new StringBuilder();
            if (item.getDescription() != null) sb.append(item.getDescription());
            RepairItem.Part part = item.getPart();
            if (part != null) {
                sb.append("\nPart: ").append(part.getName());
                if (part.getSku() != null) sb.append(" (").append(part.getSku()).append(")");
            }
            sb.append(String.format(Locale.ROOT, "\nEst: %.1f x %.2f = %.2f",
                    item.getEstimatedQuantity(), item.getEstimatedUnitPrice(), item.getEstimatedTotal()));
            addInfoRow(binding.itemsContainer, sb.toString().trim(), true);
        }
    }

    private void buildApprovals() {
        binding.approvalsContainer.removeAllViews();
        List<RepairRequest.Approval> approvals = current.getApprovals();
        if (approvals == null || approvals.isEmpty()) {
            binding.cardApprovals.setVisibility(View.GONE);
            return;
        }
        binding.cardApprovals.setVisibility(View.VISIBLE);
        for (RepairRequest.Approval a : approvals) {
            StringBuilder sb = new StringBuilder();
            String stage = a.getStage() != null ? a.getStage() : "Approval";
            sb.append(stage);
            if (a.getActorName() != null) sb.append(" — ").append(a.getActorName());
            if (a.getStatus() != null) sb.append("\nStatus: ").append(a.getStatus());
            if (a.getComment() != null && !a.getComment().isEmpty()) sb.append("\nComment: ").append(a.getComment());
            if (a.getCreatedAt() != null) sb.append("\n").append(DateUtils.dateTime(a.getCreatedAt()));
            addInfoRow(binding.approvalsContainer, sb.toString().trim(), false);
        }
    }

    private void buildAssignments() {
        binding.assignmentsContainer.removeAllViews();
        List<RepairRequest.Assignment> assignments = current.getAssignments();
        if (assignments == null || assignments.isEmpty()) {
            binding.cardAssignments.setVisibility(View.GONE);
            return;
        }
        binding.cardAssignments.setVisibility(View.VISIBLE);
        for (RepairRequest.Assignment a : assignments) {
            StringBuilder sb = new StringBuilder();
            if (a.getMechanicName() != null) sb.append(a.getMechanicName());
            if (a.getStatus() != null) sb.append("  •  ").append(a.getStatus());
            if (a.getInstructions() != null) sb.append("\n").append(a.getInstructions());
            if (a.getAssignedAt() != null) sb.append("\nAssigned: ").append(DateUtils.dateTime(a.getAssignedAt()));
            if (a.getCompletedNote() != null) sb.append("\nNote: ").append(a.getCompletedNote());
            addInfoRow(binding.assignmentsContainer, sb.toString().trim(), false);
        }
    }

    private void buildRelease() {
        RepairRequest.Release release = current.getRelease();
        if (release == null) {
            binding.cardRelease.setVisibility(View.GONE);
            return;
        }
        binding.cardRelease.setVisibility(View.VISIBLE);
        StringBuilder sb = new StringBuilder();
        sb.append("Checklist completed: ").append(release.isChecklistCompleted() ? "Yes" : "No");
        if (release.getOdometerAtRelease() != null) sb.append("\nOdometer: ").append(release.getOdometerAtRelease());
        if (release.getUnresolvedIssues() != null && !release.getUnresolvedIssues().isEmpty())
            sb.append("\nUnresolved issues: ").append(release.getUnresolvedIssues());
        if (release.getReleasedAt() != null) sb.append("\nReleased: ").append(DateUtils.dateTime(release.getReleasedAt()));
        binding.tvReleaseInfo.setText(sb.toString());
    }

    private void addInfoRow(LinearLayout container, String text, boolean bottomLine) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextSize(14);
        tv.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
        tv.setLineSpacing(0, 1.1f);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        if (bottomLine) lp.bottomMargin = 12;
        container.addView(tv, lp);
    }

    private void submitRequest() {
        binding.btnSubmit.setEnabled(false);
        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.POST, ApiConfig.repairRequestSubmit(requestId), null,
                response -> {
                    Toast.makeText(this, "Request submitted", Toast.LENGTH_SHORT).show();
                    AnalyticsHelper.logEvent(this, "repair_request_submitted");
                    loadDetail();
                },
                error -> {
                    binding.btnSubmit.setEnabled(true);
                    Toast.makeText(this, errorMessage(error, "Failed to submit"), Toast.LENGTH_SHORT).show();
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

    private void confirmCancel() {
        new android.app.AlertDialog.Builder(this)
                .setTitle("Cancel Request")
                .setMessage("Cancel this repair request?")
                .setPositiveButton("Cancel Request", (d, w) -> cancelRequest())
                .setNegativeButton("Keep", null)
                .show();
    }

    private void cancelRequest() {
        binding.btnCancel.setEnabled(false);
        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.POST, ApiConfig.repairRequestCancel(requestId), null,
                response -> {
                    Toast.makeText(this, "Request cancelled", Toast.LENGTH_SHORT).show();
                    AnalyticsHelper.logEvent(this, "repair_request_cancelled");
                    loadDetail();
                },
                error -> {
                    binding.btnCancel.setEnabled(true);
                    Toast.makeText(this, errorMessage(error, "Failed to cancel"), Toast.LENGTH_SHORT).show();
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

    private RepairRequest parseRequest(JSONObject data) {
        if (data == null) return null;
        RepairRequest r = new RepairRequest();
        r.setId(data.optInt("id"));
        r.setReference(data.optString("reference"));
        r.setType(data.optString("type"));
        r.setPriority(data.optString("priority"));
        r.setStatus(data.optString("status"));
        r.setDescription(data.optString("description"));
        r.setCreatedAt(data.optString("created_at"));
        r.setUpdatedAt(data.optString("updated_at"));
        r.setSubmittedAt(data.isNull("submitted_at") ? null : data.optString("submitted_at"));
        r.setLatitude(data.optDouble("latitude", 0));
        r.setLongitude(data.optDouble("longitude", 0));

        if (data.has("vehicle") && !data.isNull("vehicle")) {
            try {
                JSONObject v = data.getJSONObject("vehicle");
                RepairRequest.Vehicle vehicle = new RepairRequest.Vehicle();
                vehicle.setId(v.optInt("id"));
                vehicle.setPlateNumber(v.optString("plate_number"));
                vehicle.setStatus(v.optString("status"));
                r.setVehicle(vehicle);
            } catch (JSONException ignored) {}
        }

        if (data.has("driver") && !data.isNull("driver")) {
            try {
                JSONObject d = data.getJSONObject("driver");
                RepairRequest.Driver driver = new RepairRequest.Driver();
                driver.setId(d.optInt("id"));
                driver.setName(d.optString("name"));
                driver.setPhone(d.optString("phone"));
                r.setDriver(driver);
            } catch (JSONException ignored) {}
        }

        r.setItems(parseItems(data.optJSONArray("items")));
        r.setApprovals(parseApprovals(data.optJSONArray("approvals")));
        r.setAssignments(parseAssignments(data.optJSONArray("assignments")));
        r.setRelease(parseRelease(data.optJSONObject("release")));
        return r;
    }

    private List<RepairItem> parseItems(JSONArray arr) {
        List<RepairItem> list = new ArrayList<>();
        if (arr == null) return list;
        for (int i = 0; i < arr.length(); i++) {
            try {
                JSONObject j = arr.getJSONObject(i);
                RepairItem item = new RepairItem();
                item.setId(j.optInt("id"));
                item.setDescription(j.optString("description"));
                item.setEstimatedQuantity(j.optDouble("estimated_quantity", 0));
                item.setEstimatedUnitPrice(j.optDouble("estimated_unit_price", 0));
                item.setEstimatedTotal(j.optDouble("estimated_total", 0));
                item.setActualQuantity(j.optDouble("actual_quantity", 0));
                item.setActualUnitPrice(j.optDouble("actual_unit_price", 0));
                if (j.has("part") && !j.isNull("part")) {
                    JSONObject p = j.getJSONObject("part");
                    RepairItem.Part part = new RepairItem.Part();
                    part.setId(p.optInt("id"));
                    part.setName(p.optString("name"));
                    part.setSku(p.optString("sku"));
                    item.setPart(part);
                }
                list.add(item);
            } catch (JSONException ignored) {}
        }
        return list;
    }

    private List<RepairRequest.Approval> parseApprovals(JSONArray arr) {
        List<RepairRequest.Approval> list = new ArrayList<>();
        if (arr == null) return list;
        for (int i = 0; i < arr.length(); i++) {
            try {
                JSONObject j = arr.getJSONObject(i);
                RepairRequest.Approval a = new RepairRequest.Approval();
                a.setId(j.optInt("id"));
                a.setStage(j.optString("stage"));
                a.setStatus(j.optString("status"));
                a.setComment(j.isNull("comment") ? null : j.optString("comment"));
                a.setCreatedAt(j.optString("created_at"));
                if (j.has("actor") && !j.isNull("actor")) {
                    JSONObject actor = j.getJSONObject("actor");
                    a.setActorName(actor.optString("name"));
                }
                list.add(a);
            } catch (JSONException ignored) {}
        }
        return list;
    }

    private List<RepairRequest.Assignment> parseAssignments(JSONArray arr) {
        List<RepairRequest.Assignment> list = new ArrayList<>();
        if (arr == null) return list;
        for (int i = 0; i < arr.length(); i++) {
            try {
                JSONObject j = arr.getJSONObject(i);
                RepairRequest.Assignment a = new RepairRequest.Assignment();
                a.setId(j.optInt("id"));
                a.setStatus(j.optString("status"));
                a.setInstructions(j.isNull("instructions") ? null : j.optString("instructions"));
                a.setAssignedAt(j.optString("assigned_at"));
                a.setStartedAt(j.isNull("started_at") ? null : j.optString("started_at"));
                a.setCompletedAt(j.isNull("completed_at") ? null : j.optString("completed_at"));
                a.setCompletedNote(j.isNull("completed_note") ? null : j.optString("completed_note"));
                if (j.has("mechanic") && !j.isNull("mechanic")) {
                    JSONObject m = j.getJSONObject("mechanic");
                    a.setMechanicName(m.optString("name"));
                }
                list.add(a);
            } catch (JSONException ignored) {}
        }
        return list;
    }

    private RepairRequest.Release parseRelease(JSONObject j) {
        if (j == null) return null;
        RepairRequest.Release rel = new RepairRequest.Release();
        rel.setId(j.optInt("id"));
        rel.setChecklistCompleted(j.optBoolean("checklist_completed", false));
        rel.setUnresolvedIssues(j.isNull("unresolved_issues") ? null : j.optString("unresolved_issues"));
        rel.setOdometerAtRelease(j.isNull("odometer_at_release") ? null : String.valueOf(j.opt("odometer_at_release")));
        rel.setReleasedAt(j.optString("released_at"));
        if (rel.getReleasedAt().isEmpty()) rel.setReleasedAt(j.optString("created_at"));
        return rel;
    }

    private int statusColor(String status) {
        if (status == null) return R.color.repair_status_draft;
        switch (status.toLowerCase(Locale.ROOT)) {
            case "draft": return R.color.repair_status_draft;
            case "pending_approval":
            case "pending_ops_approval": return R.color.repair_status_pending;
            case "approved": return R.color.repair_status_approved;
            case "in_progress": return R.color.repair_status_in_progress;
            case "completed": return R.color.repair_status_completed;
            case "released": return R.color.repair_status_released;
            case "cancelled": return R.color.repair_status_cancelled;
            default: return R.color.repair_status_draft;
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
