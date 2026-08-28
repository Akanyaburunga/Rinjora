package rw.martinhardware.mymartin.data;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Response;
import rw.martinhardware.mymartin.network.ApiEnvelope;
import rw.martinhardware.mymartin.network.RinjoraApi;
import rw.martinhardware.mymartin.network.RinjoraApiClient;
import rw.martinhardware.mymartin.network.dto.SubmissionDto;

/**
 * User-generated riddle submissions (plan §8): submit a new riddle for admin
 * review ({@code POST /submissions/riddles}) and list my submissions
 * ({@code GET /submissions/riddles}) with status + rejection reason.
 */
public class RinjoraSubmissionRepository {

    private static final String TAG = "RinjoraSubmissionRepo";

    public interface Callback<T> {
        void onSuccess(T data);

        void onAuthError();

        void onError(String message);
    }

    private final RinjoraApi api;

    public RinjoraSubmissionRepository(Context context) {
        this.api = RinjoraApiClient.get(context).api();
    }

    /** POST /submissions/riddles — submit a new riddle for review (plan §8.1). */
    public void create(String question, String answer, String difficulty, String riddleType,
                       String hint, String hint2, String source, final Callback<SubmissionDto> callback) {
        Map<String, Object> body = new HashMap<>();
        body.put("question", question);
        body.put("answer", answer);
        body.put("difficulty", difficulty);
        body.put("riddle_type", riddleType);
        if (hint != null && !hint.trim().isEmpty()) body.put("hint", hint.trim());
        if (hint2 != null && !hint2.trim().isEmpty()) body.put("hint2", hint2.trim());
        body.put("source", source);

        api.submitRiddle(body).enqueue(new retrofit2.Callback<ApiEnvelope<SubmissionDto>>() {
            @Override
            public void onResponse(@NonNull Call<ApiEnvelope<SubmissionDto>> call,
                                   @NonNull Response<ApiEnvelope<SubmissionDto>> response) {
                ApiEnvelope<SubmissionDto> envelope = response.body();
                if (response.isSuccessful() && envelope != null && envelope.isSuccess()) {
                    if (callback != null) callback.onSuccess(envelope.getData());
                } else if (response.code() == 401) {
                    if (callback != null) callback.onAuthError();
                } else {
                    if (callback != null) callback.onError(extractError(response, envelope));
                }
            }

            @Override
            public void onFailure(@NonNull Call<ApiEnvelope<SubmissionDto>> call, @NonNull Throwable t) {
                Log.e(TAG, "submit failed", t);
                if (callback != null) callback.onError(msg(t));
            }
        });
    }

    /** GET /submissions/riddles — my submissions (plan §8.2). */
    public void list(final Callback<List<SubmissionDto>> callback) {
        api.submissions().enqueue(new retrofit2.Callback<ApiEnvelope<List<SubmissionDto>>>() {
            @Override
            public void onResponse(@NonNull Call<ApiEnvelope<List<SubmissionDto>>> call,
                                   @NonNull Response<ApiEnvelope<List<SubmissionDto>>> response) {
                ApiEnvelope<List<SubmissionDto>> envelope = response.body();
                if (response.isSuccessful() && envelope != null && envelope.isSuccess()) {
                    if (callback != null) callback.onSuccess(envelope.getData());
                } else if (response.code() == 401) {
                    if (callback != null) callback.onAuthError();
                } else {
                    if (callback != null) callback.onError(extractError(response, envelope));
                }
            }

            @Override
            public void onFailure(@NonNull Call<ApiEnvelope<List<SubmissionDto>>> call, @NonNull Throwable t) {
                Log.e(TAG, "list failed", t);
                if (callback != null) callback.onError(msg(t));
            }
        });
    }

    private String msg(Throwable t) {
        return t.getMessage() == null ? "Network error" : t.getMessage();
    }

    private String extractError(Response<?> response, @Nullable ApiEnvelope<?> envelope) {
        if (envelope != null && envelope.getMessage() != null && !envelope.getMessage().isEmpty()) {
            return envelope.getMessage();
        }
        ResponseBody body = response.errorBody();
        if (body != null) {
            try {
                String raw = body.string();
                com.google.gson.JsonObject obj = com.google.gson.JsonParser.parseString(raw).getAsJsonObject();
                if (obj.has("message")) {
                    return obj.get("message").getAsString();
                }
            } catch (IOException | RuntimeException ignored) {
                // fall through
            }
        }
        return "Request failed (HTTP " + response.code() + ")";
    }
}
