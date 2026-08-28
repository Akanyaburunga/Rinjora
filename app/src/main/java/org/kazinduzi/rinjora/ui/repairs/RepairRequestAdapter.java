package org.kazinduzi.rinjora.ui.repairs;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.kazinduzi.rinjora.R;
import org.kazinduzi.rinjora.models.RepairRequest;
import org.kazinduzi.rinjora.util.DateUtils;

public class RepairRequestAdapter extends RecyclerView.Adapter<RepairRequestAdapter.ViewHolder> {

    private List<RepairRequest> requests = new ArrayList<>();
    private OnRepairClickListener listener;

    public interface OnRepairClickListener {
        void onRepairClick(RepairRequest request);
    }

    public void setRequests(List<RepairRequest> requests) {
        this.requests = requests;
        notifyDataSetChanged();
    }

    public void setOnRepairClickListener(OnRepairClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_repair_request, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder h, int pos) {
        RepairRequest r = requests.get(pos);
        h.tvReference.setText(r.getReference() != null ? r.getReference() : "#" + r.getId());
        h.tvDate.setText(DateUtils.date(r.getSubmittedAt() != null ? r.getSubmittedAt() : r.getCreatedAt()));
        h.tvDescription.setText(r.getDescription());

        RepairRequest.Vehicle v = r.getVehicle();
        h.tvVehicle.setText(v != null ? v.getPlateNumber() : "");

        String type = r.getType() != null ? r.getType() : "";
        h.tvType.setText(type.toUpperCase(Locale.ROOT));

        String priority = r.getPriority() != null ? r.getPriority() : "";
        h.tvPriority.setText(priority.toUpperCase(Locale.ROOT));
        h.tvPriority.setTextColor(ContextCompat.getColor(h.itemView.getContext(), priorityColor(priority)));

        String status = r.getStatus() != null ? r.getStatus() : "";
        h.tvStatus.setText(status.toUpperCase(Locale.ROOT).replace('_', ' '));
        int statusColor = ContextCompat.getColor(h.itemView.getContext(), statusColor(status));
        h.tvStatus.setTextColor(statusColor);
        h.tvStatus.setBackgroundTintList(android.content.res.ColorStateList.valueOf(statusColor & 0x1FFFFFFF | 0x22000000));

        h.itemView.setOnClickListener(vv -> {
            if (listener != null) listener.onRepairClick(r);
        });
    }

    @Override
    public int getItemCount() { return requests.size(); }

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

    private int priorityColor(String priority) {
        if (priority == null) return R.color.text_secondary;
        switch (priority.toLowerCase(Locale.ROOT)) {
            case "high":
            case "urgent":
            case "critical": return R.color.priority_high;
            case "medium":
            case "normal": return R.color.priority_medium;
            case "low": return R.color.priority_low;
            default: return R.color.text_secondary;
        }
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvReference, tvDate, tvVehicle, tvStatus, tvType, tvPriority, tvDescription;
        ViewHolder(@NonNull View v) {
            super(v);
            tvReference = v.findViewById(R.id.tv_repair_reference);
            tvDate = v.findViewById(R.id.tv_repair_date);
            tvVehicle = v.findViewById(R.id.tv_repair_vehicle);
            tvStatus = v.findViewById(R.id.tv_repair_status);
            tvType = v.findViewById(R.id.tv_repair_type);
            tvPriority = v.findViewById(R.id.tv_repair_priority);
            tvDescription = v.findViewById(R.id.tv_repair_description);
        }
    }
}
