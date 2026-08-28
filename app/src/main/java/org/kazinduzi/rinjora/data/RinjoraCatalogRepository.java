package org.kazinduzi.rinjora.data;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import io.objectbox.Box;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Response;
import org.kazinduzi.rinjora.MyApp;
import org.kazinduzi.rinjora.entities.RinjoraCatalogSnapshot;
import org.kazinduzi.rinjora.network.ApiEnvelope;
import org.kazinduzi.rinjora.network.RinjoraApi;
import org.kazinduzi.rinjora.network.RinjoraApiClient;
import org.kazinduzi.rinjora.network.dto.CategoryDto;
import org.kazinduzi.rinjora.network.dto.RiddleDto;

/**
 * Offline-first repository for the riddle catalog (plan Phase 10/K).
 *
 * The Play screen renders instantly from ObjectBox on open, then refreshes in the
 * background. To keep offline filtering useful, this caches the <b>unfiltered</b>
 * {@code GET /riddles} list and {@code GET /riddles/categories}; when a filtered
 * refresh fails (offline) the cached list is filtered locally by difficulty.
 */
public class RinjoraCatalogRepository {

    private static final String TAG = "RinjoraCatalogRepository";
    private final Gson gson = new Gson();
    private final Type riddleListType = new TypeToken<List<RiddleDto>>() {}.getType();
    private final Type categoryListType = new TypeToken<List<CategoryDto>>() {}.getType();

    private static final String RIDDLE_ROW_KEY = "catalog";
    private static final String CATEGORY_ROW_KEY = RinjoraCatalogSnapshot.KIND_CATEGORIES;

    public interface Callback {
        void onSuccess();

        void onAuthError();

        void onError(String message);
    }

    private final Context context;
    private final RinjoraApi api;

    public RinjoraCatalogRepository(Context context) {
        this.context = context.getApplicationContext();
        this.api = RinjoraApiClient.get(context).api();
    }

    /** GET /riddles (unfiltered) + GET /riddles/categories, caching each. */
    public void refresh(Callback callback) {
        fetchRiddles(callback);
        fetchCategories(callback);
    }

    /** GET /riddles (unfiltered) then cache, and only then notify. */
    public void fetchRiddles(final Callback callback) {
        api.riddles().enqueue(new retrofit2.Callback<ApiEnvelope<List<RiddleDto>>>() {
            @Override
            public void onResponse(@NonNull Call<ApiEnvelope<List<RiddleDto>>> call,
                                   @NonNull Response<ApiEnvelope<List<RiddleDto>>> response) {
                ApiEnvelope<List<RiddleDto>> envelope = response.body();
                if (response.isSuccessful() && envelope != null && envelope.isSuccess()) {
                    save(RinjoraCatalogSnapshot.KIND_RIDDLES, RIDDLE_ROW_KEY, gson.toJson(envelope.getData()));
                    if (callback != null) callback.onSuccess();
                } else if (response.code() == 401) {
                    if (callback != null) callback.onAuthError();
                } else {
                    if (callback != null) callback.onError(extractError(response, envelope));
                }
            }

            @Override
            public void onFailure(@NonNull Call<ApiEnvelope<List<RiddleDto>>> call, @NonNull Throwable t) {
                Log.e(TAG, "fetchRiddles failed", t);
                if (callback != null) callback.onError(t.getMessage() == null ? "Network error" : t.getMessage());
            }
        });
    }

    /** GET /riddles/categories then cache, and only then notify. */
    public void fetchCategories(final Callback callback) {
        api.categories().enqueue(new retrofit2.Callback<ApiEnvelope<List<CategoryDto>>>() {
            @Override
            public void onResponse(@NonNull Call<ApiEnvelope<List<CategoryDto>>> call,
                                   @NonNull Response<ApiEnvelope<List<CategoryDto>>> response) {
                ApiEnvelope<List<CategoryDto>> envelope = response.body();
                if (response.isSuccessful() && envelope != null && envelope.isSuccess()) {
                    save(RinjoraCatalogSnapshot.KIND_CATEGORIES, CATEGORY_ROW_KEY, gson.toJson(envelope.getData()));
                    if (callback != null) callback.onSuccess();
                } else if (response.code() == 401) {
                    if (callback != null) callback.onAuthError();
                } else {
                    if (callback != null) callback.onError(extractError(response, envelope));
                }
            }

            @Override
            public void onFailure(@NonNull Call<ApiEnvelope<List<CategoryDto>>> call, @NonNull Throwable t) {
                Log.e(TAG, "fetchCategories failed", t);
                if (callback != null) callback.onError(t.getMessage() == null ? "Network error" : t.getMessage());
            }
        });
    }

    /** Cached riddles (unfiltered), or an empty list when never synced / offline. */
    @NonNull
    public List<RiddleDto> getCachedRiddles() {
        RinjoraCatalogSnapshot snap = findOne(RinjoraCatalogSnapshot.KIND_RIDDLES, RIDDLE_ROW_KEY);
        if (snap == null) return new ArrayList<>();
        List<RiddleDto> parsed = safeParse(riddleListType, snap.getRawJson());
        return parsed != null ? parsed : new ArrayList<>();
    }

    /** Cached categories, or an empty list when never synced / offline. */
    @NonNull
    public List<CategoryDto> getCachedCategories() {
        RinjoraCatalogSnapshot snap = findOne(RinjoraCatalogSnapshot.KIND_CATEGORIES, CATEGORY_ROW_KEY);
        if (snap == null) return new ArrayList<>();
        List<CategoryDto> parsed = safeParse(categoryListType, snap.getRawJson());
        return parsed != null ? parsed : new ArrayList<>();
    }

    /** ms since the riddle catalog was last synced; -1 when never synced. */
    public long getStalenessMs() {
        RinjoraCatalogSnapshot snap = findOne(RinjoraCatalogSnapshot.KIND_RIDDLES, RIDDLE_ROW_KEY);
        return snap != null ? System.currentTimeMillis() - snap.getFetchedAt() : -1;
    }

    private Box<RinjoraCatalogSnapshot> box() {
        return ((MyApp) context.getApplicationContext()).getBoxStore().boxFor(RinjoraCatalogSnapshot.class);
    }

    @Nullable
    private RinjoraCatalogSnapshot findOne(String kind, String key) {
        try {
            // ObjectBox has no String "equal" overload, so match the (tiny) row
            // set in memory. This cache keeps at most one row per kind.
            for (RinjoraCatalogSnapshot snap : box().getAll()) {
                if (kind.equals(snap.getKind()) && key.equals(snap.getKey())) {
                    return snap;
                }
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    private void save(String kind, String key, String rawJson) {
        Box<RinjoraCatalogSnapshot> box = box();
        for (RinjoraCatalogSnapshot old : box.getAll()) {
            if (kind.equals(old.getKind())) {
                box.remove(old);
            }
        }
        RinjoraCatalogSnapshot snap = new RinjoraCatalogSnapshot();
        snap.setKind(kind);
        snap.setKey(key);
        snap.setFetchedAt(System.currentTimeMillis());
        snap.setRawJson(rawJson);
        box.put(snap);
    }

    private <T> T safeParse(Type type, String rawJson) {
        try {
            return gson.fromJson(rawJson, type);
        } catch (Exception e) {
            return null;
        }
    }

    private static String extractError(Response<?> response, ApiEnvelope<?> envelope) {
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
