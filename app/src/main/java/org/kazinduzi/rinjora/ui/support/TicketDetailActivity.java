package org.kazinduzi.rinjora.ui.support;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.android.volley.Request;
import com.android.volley.toolbox.JsonObjectRequest;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.objectbox.Box;
import org.kazinduzi.rinjora.BaseActivity;
import org.kazinduzi.rinjora.MyApp;
import org.kazinduzi.rinjora.databinding.ActivityTicketDetailBinding;
import org.kazinduzi.rinjora.util.DateUtils;
import org.kazinduzi.rinjora.util.AnalyticsHelper;
import org.kazinduzi.rinjora.entities.User;
import org.kazinduzi.rinjora.entities.User_;
import org.kazinduzi.rinjora.models.Ticket;
import org.kazinduzi.rinjora.models.TicketMessage;
import org.kazinduzi.rinjora.network.ApiConfig;
import org.kazinduzi.rinjora.network.VolleySingleton;

public class TicketDetailActivity extends BaseActivity {

    private ActivityTicketDetailBinding binding;
    private VolleySingleton volley;
    private String token;
    private int ticketId;
    private Ticket currentTicket;
    private MessageAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityTicketDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        ticketId = getIntent().getIntExtra("ticket_id", 0);
        volley = VolleySingleton.getInstance(this);
        token = getAuthToken();

        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Ticket #" + ticketId);
        }
        binding.toolbar.setNavigationOnClickListener(v -> finish());

        adapter = new MessageAdapter();
        binding.rvMessages.setLayoutManager(new LinearLayoutManager(this));
        binding.rvMessages.setAdapter(adapter);

        binding.btnSend.setOnClickListener(v -> sendMessage());

        loadTicketDetail();
    }

    private void loadTicketDetail() {
        binding.progressBar.setVisibility(View.VISIBLE);

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.GET, ApiConfig.supportTicketDetail(ticketId), null,
                response -> {
                    binding.progressBar.setVisibility(View.GONE);
                    currentTicket = parseTicket(response);
                    if (currentTicket != null) {
                        String ref = currentTicket.getReference() != null ? currentTicket.getReference() : "#" + currentTicket.getId();
                        binding.tvReference.setText(ref);
                        binding.tvSubject.setText(currentTicket.getTitle());
                        binding.tvStatus.setText(currentTicket.getStatus());
                        binding.tvDescription.setText(currentTicket.getDescription());

                        StringBuilder info = new StringBuilder();
                        if (currentTicket.getPriority() != null) {
                            info.append("Priority: ").append(currentTicket.getPriority()).append("\n");
                        }
                        if (currentTicket.getCategory() != null) {
                            info.append("Category: ").append(currentTicket.getCategory().getName()).append("\n");
                        }
                        if (currentTicket.getUser() != null) {
                            info.append("Reported by: ").append(currentTicket.getUser().getName()).append("\n");
                        }
                        if (currentTicket.getAssignee() != null) {
                            info.append("Assigned to: ").append(currentTicket.getAssignee().getName());
                        }
                        if (info.length() > 0) {
                            binding.tvInfo.setText(info.toString().trim());
                            binding.tvInfo.setVisibility(View.VISIBLE);
                        }

                        if (currentTicket.getEvents() != null && !currentTicket.getEvents().isEmpty()) {
                            StringBuilder events = new StringBuilder("Timeline:\n");
                            for (Ticket.Event e : currentTicket.getEvents()) {
                                String actor = e.getActor() != null ? e.getActor().getName() : "System";
                                events.append("• ").append(actor).append(" - ").append(e.getType());
                                if (e.getCreatedAt() != null) {
                                    events.append(" (").append(DateUtils.dateTime(e.getCreatedAt())).append(")");
                                }
                                events.append("\n");
                            }
                            binding.tvEvents.setText(events.toString().trim());
                            binding.tvEvents.setVisibility(View.VISIBLE);
                        } else {
                            binding.tvEvents.setVisibility(View.GONE);
                        }

                        if (currentTicket.getMessages() != null && !currentTicket.getMessages().isEmpty()) {
                            adapter.setMessages(currentTicket.getMessages());
                            binding.rvMessages.scrollToPosition(currentTicket.getMessages().size() - 1);
                        }
                        binding.contentView.setVisibility(View.VISIBLE);
                    }
                },
                error -> {
                    binding.progressBar.setVisibility(View.GONE);
                    binding.tvError.setVisibility(View.VISIBLE);
                    Toast.makeText(this, "Failed to load ticket", Toast.LENGTH_SHORT).show();
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

    private void sendMessage() {
        String msg = binding.etMessage.getText().toString().trim();
        if (msg.isEmpty()) return;

        binding.btnSend.setEnabled(false);

        JSONObject body = new JSONObject();
        try { body.put("message", msg); } catch (JSONException ignored) {}

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.POST, ApiConfig.supportTicketMessages(ticketId), body,
                response -> {
                    binding.etMessage.setText("");
                    binding.btnSend.setEnabled(true);
                    AnalyticsHelper.logEvent(this, "support_message_sent");
                    loadTicketDetail();
                },
                error -> {
                    binding.btnSend.setEnabled(true);
                    Toast.makeText(this, "Failed to send message", Toast.LENGTH_SHORT).show();
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

    private Ticket parseTicket(JSONObject response) {
        try {
            JSONObject data = response.optJSONObject("data");
            if (data == null) data = response;

            Ticket t = new Ticket();
            t.setId(data.optInt("id"));
            t.setReference(data.optString("reference"));
            t.setTitle(data.optString("title"));
            t.setDescription(data.optString("description"));
            t.setStatus(data.optString("status"));
            t.setPriority(data.optString("priority"));
            t.setCreatedAt(data.optString("created_at"));
            t.setUpdatedAt(data.optString("updated_at"));
            t.setResolvedAt(data.optString("resolved_at", null));
            t.setClosedAt(data.optString("closed_at", null));

            if (data.has("user")) {
                JSONObject u = data.getJSONObject("user");
                Ticket.UserSummary user = new Ticket.UserSummary();
                user.setId(u.optInt("id"));
                user.setName(u.optString("name"));
                user.setEmail(u.optString("email"));
                t.setUser(user);
            }

            if (data.has("category")) {
                JSONObject c = data.getJSONObject("category");
                Ticket.Category cat = new Ticket.Category();
                cat.setId(c.optInt("id"));
                cat.setName(c.optString("name"));
                t.setCategory(cat);
            }

            if (data.has("assignee") && !data.isNull("assignee")) {
                JSONObject a = data.getJSONObject("assignee");
                Ticket.UserSummary assignee = new Ticket.UserSummary();
                assignee.setId(a.optInt("id"));
                assignee.setName(a.optString("name"));
                assignee.setEmail(a.optString("email"));
                t.setAssignee(assignee);
            }

            List<TicketMessage> msgs = new ArrayList<>();
            if (data.has("messages")) {
                JSONArray arr = data.getJSONArray("messages");
                for (int i = 0; i < arr.length(); i++) {
                    JSONObject m = arr.getJSONObject(i);
                    TicketMessage tm = new TicketMessage();
                    tm.setId(m.optInt("id"));
                    tm.setMessage(m.optString("message"));
                    tm.setCreatedAt(m.optString("created_at"));
                    if (m.has("author") && !m.isNull("author")) {
                        JSONObject a = m.getJSONObject("author");
                        TicketMessage.AuthorSummary author = new TicketMessage.AuthorSummary();
                        author.setId(a.optInt("id"));
                        author.setName(a.optString("name"));
                        author.setEmail(a.optString("email"));
                        author.setType(a.optString("type"));
                        tm.setAuthor(author);
                    }
                    msgs.add(tm);
                }
            }
            t.setMessages(msgs);

            List<Ticket.Event> events = new ArrayList<>();
            if (data.has("events")) {
                JSONArray arr = data.getJSONArray("events");
                for (int i = 0; i < arr.length(); i++) {
                    JSONObject e = arr.getJSONObject(i);
                    Ticket.Event ev = new Ticket.Event();
                    ev.setId(e.optInt("id"));
                    ev.setType(e.optString("type"));
                    ev.setCreatedAt(e.optString("created_at"));
                    if (e.has("actor") && !e.isNull("actor")) {
                        JSONObject a = e.getJSONObject("actor");
                        Ticket.UserSummary actor = new Ticket.UserSummary();
                        actor.setId(a.optInt("id"));
                        actor.setName(a.optString("name"));
                        actor.setEmail(a.optString("email"));
                        ev.setActor(actor);
                    }
                    events.add(ev);
                }
            }
            t.setEvents(events);

            return t;
        } catch (JSONException e) {
            return null;
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
