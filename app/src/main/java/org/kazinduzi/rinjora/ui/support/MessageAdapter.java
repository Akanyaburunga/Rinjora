package org.kazinduzi.rinjora.ui.support;

import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import org.kazinduzi.rinjora.R;
import org.kazinduzi.rinjora.models.TicketMessage;
import org.kazinduzi.rinjora.util.DateUtils;

public class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.ViewHolder> {

    private List<TicketMessage> messages = new ArrayList<>();

    public void setMessages(List<TicketMessage> messages) {
        this.messages = messages;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_message, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder h, int pos) {
        TicketMessage m = messages.get(pos);
        h.tvMessage.setText(m.getMessage());
        h.tvTime.setText(DateUtils.time(m.getCreatedAt()));

        boolean isUser = m.getAuthor() != null && "user".equals(m.getAuthor().getType());

        if (m.getAuthor() != null && m.getAuthor().getName() != null) {
            h.tvAuthor.setText(m.getAuthor().getName());
            h.tvAuthor.setVisibility(View.VISIBLE);
        } else {
            h.tvAuthor.setVisibility(View.GONE);
        }

        FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) h.bubble.getLayoutParams();
        if (isUser) {
            params.gravity = Gravity.END;
            h.bubble.setBackgroundResource(R.drawable.bg_message_user);
        } else {
            params.gravity = Gravity.START;
            h.bubble.setBackgroundResource(R.drawable.bg_message_support);
        }
        h.bubble.setLayoutParams(params);
    }

    @Override
    public int getItemCount() { return messages.size(); }

    static class ViewHolder extends RecyclerView.ViewHolder {
        View bubble;
        TextView tvMessage, tvTime, tvAuthor;
        ViewHolder(@NonNull View v) {
            super(v);
            bubble = v.findViewById(R.id.message_bubble);
            tvMessage = v.findViewById(R.id.tv_message);
            tvTime = v.findViewById(R.id.tv_message_time);
            tvAuthor = v.findViewById(R.id.tv_message_author);
        }
    }
}
