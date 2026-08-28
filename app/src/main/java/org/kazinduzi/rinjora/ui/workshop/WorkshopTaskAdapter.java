package org.kazinduzi.rinjora.ui.workshop;

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
import org.kazinduzi.rinjora.models.RepairTask;
import org.kazinduzi.rinjora.util.DateUtils;

public class WorkshopTaskAdapter extends RecyclerView.Adapter<WorkshopTaskAdapter.ViewHolder> {

    private List<RepairTask> tasks = new ArrayList<>();
    private OnTaskClickListener listener;

    public interface OnTaskClickListener {
        void onTaskClick(RepairTask task);
    }

    public void setTasks(List<RepairTask> tasks) {
        this.tasks = tasks;
        notifyDataSetChanged();
    }

    public void setOnTaskClickListener(OnTaskClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_repair_task, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder h, int pos) {
        RepairTask t = tasks.get(pos);
        h.tvReference.setText(t.getReference() != null ? t.getReference() : "#" + t.getId());
        h.tvVehicle.setText(t.getVehiclePlate());
        h.tvInstructions.setText(t.getInstructions());
        h.tvDuration.setText(t.getDuration() != null ? t.getDuration() : "");
        h.tvDate.setText(DateUtils.date(t.getAssignedAt()));

        String status = t.getStatus() != null ? t.getStatus() : "";
        h.tvStatus.setText(status.toUpperCase(Locale.ROOT).replace('_', ' '));
        int statusColor = ContextCompat.getColor(h.itemView.getContext(), statusColor(status));
        h.tvStatus.setTextColor(statusColor);
        h.tvStatus.setBackgroundTintList(android.content.res.ColorStateList.valueOf(statusColor & 0x1FFFFFFF | 0x22000000));

        h.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onTaskClick(t);
        });
    }

    @Override
    public int getItemCount() { return tasks.size(); }

    private int statusColor(String status) {
        if (status == null) return R.color.task_status_assigned;
        switch (status.toLowerCase(Locale.ROOT)) {
            case "assigned": return R.color.task_status_assigned;
            case "in_progress": return R.color.task_status_in_progress;
            case "completed": return R.color.task_status_completed;
            default: return R.color.task_status_assigned;
        }
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvReference, tvStatus, tvVehicle, tvInstructions, tvDuration, tvDate;
        ViewHolder(@NonNull View v) {
            super(v);
            tvReference = v.findViewById(R.id.tv_task_reference);
            tvStatus = v.findViewById(R.id.tv_task_status);
            tvVehicle = v.findViewById(R.id.tv_task_vehicle);
            tvInstructions = v.findViewById(R.id.tv_task_instructions);
            tvDuration = v.findViewById(R.id.tv_task_duration);
            tvDate = v.findViewById(R.id.tv_task_date);
        }
    }
}
