package org.kazinduzi.rinjora.ui.support;

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
import org.kazinduzi.rinjora.models.Ticket;
import org.kazinduzi.rinjora.util.DateUtils;

public class SupportTicketAdapter extends RecyclerView.Adapter<SupportTicketAdapter.ViewHolder> {

    private List<Ticket> tickets = new ArrayList<>();
    private OnTicketClickListener listener;

    public interface OnTicketClickListener {
        void onTicketClick(Ticket ticket);
    }

    public void setTickets(List<Ticket> tickets) {
        this.tickets = tickets;
        notifyDataSetChanged();
    }

    public void setOnTicketClickListener(OnTicketClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_support_ticket, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder h, int pos) {
        Ticket t = tickets.get(pos);
        h.tvReference.setText(t.getReference() != null ? t.getReference() : "#" + t.getId());
        h.tvTitle.setText(t.getTitle());
        h.tvDate.setText(DateUtils.date(t.getCreatedAt()));

        Ticket.Category cat = t.getCategory();
        h.tvCategory.setText(cat != null ? cat.getName() : "");

        String status = t.getStatus() != null ? t.getStatus() : "";
        h.tvStatus.setText(status.toUpperCase(Locale.ROOT));
        h.tvStatus.setTextColor(ContextCompat.getColor(h.itemView.getContext(), statusColor(status)));

        String priority = t.getPriority() != null ? t.getPriority() : "";
        h.tvPriority.setText(priority.toUpperCase(Locale.ROOT));
        h.tvPriority.setTextColor(ContextCompat.getColor(h.itemView.getContext(), priorityColor(priority)));

        h.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onTicketClick(t);
        });
    }

    @Override
    public int getItemCount() { return tickets.size(); }

    private int statusColor(String status) {
        if (status == null) return R.color.status_default;
        switch (status.toLowerCase(Locale.ROOT)) {
            case "open": return R.color.status_open;
            case "in_progress": return R.color.status_progress;
            case "pending": return R.color.status_pending;
            case "resolved": return R.color.status_resolved;
            case "closed": return R.color.status_closed;
            default: return R.color.status_default;
        }
    }

    private int priorityColor(String priority) {
        if (priority == null) return R.color.text_secondary;
        switch (priority.toLowerCase(Locale.ROOT)) {
            case "high":
            case "urgent": return R.color.priority_high;
            case "medium":
            case "normal": return R.color.priority_medium;
            case "low": return R.color.priority_low;
            default: return R.color.text_secondary;
        }
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvReference, tvTitle, tvStatus, tvPriority, tvDate, tvCategory;
        ViewHolder(@NonNull View v) {
            super(v);
            tvReference = v.findViewById(R.id.tv_ticket_reference);
            tvTitle = v.findViewById(R.id.tv_ticket_title);
            tvStatus = v.findViewById(R.id.tv_ticket_status);
            tvPriority = v.findViewById(R.id.tv_ticket_priority);
            tvDate = v.findViewById(R.id.tv_ticket_date);
            tvCategory = v.findViewById(R.id.tv_ticket_category);
        }
    }
}
