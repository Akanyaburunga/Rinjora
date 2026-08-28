package org.kazinduzi.rinjora.data;

import android.content.Context;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.toolbox.JsonObjectRequest;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.objectbox.Box;
import io.objectbox.BoxStore;
import org.kazinduzi.rinjora.MyApp;
import org.kazinduzi.rinjora.entities.DriverProfile;
import org.kazinduzi.rinjora.entities.DriverProfile_;
import org.kazinduzi.rinjora.entities.DriverTrip;
import org.kazinduzi.rinjora.entities.DriverTrip_;
import org.kazinduzi.rinjora.entities.User;
import org.kazinduzi.rinjora.entities.User_;
import org.kazinduzi.rinjora.network.ApiConfig;
import org.kazinduzi.rinjora.network.VolleySingleton;

/**
 * Offline-first repository for the driver profile screen (GET /api/mobile/profile).
 * Every fetch writes a {@link DriverProfile} (+ {@link DriverTrip} rows) to ObjectBox before
 * the UI reads it, so the profile renders instantly from cache even fully offline, then
 * refreshes in the background (WorkManager periodic sync + refresh on screen open).
 *
 * Response handling per docs/BACKEND_PROFILE_IMPLEMENTATION.md:
 *   200 -> full payload
 *   401/403 -> auth error -> back to login
 */
public class DriverProfileRepository {

    private static final String TAG = "DriverProfileRepository";

    public interface Callback {
        void onSuccess(DriverProfile profile);
        void onAuthError();
        void onError(String message);
    }

    /** GET /api/mobile/profile, then cache the result. */
    public void fetch(Context context, Callback callback) {
        String token = getAuthToken(context);
        if (token == null) {
            if (callback != null) callback.onAuthError();
            return;
        }

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.GET, ApiConfig.PROFILE, null,
                response -> {
                    try {
                        DriverProfile profile = parse(response);
                        profile.setFetchedAt(System.currentTimeMillis());
                        save(context, profile);
                        if (callback != null) callback.onSuccess(profile);
                    } catch (Exception e) {
                        Log.e(TAG, "Failed to parse profile payload", e);
                        if (callback != null) callback.onError("Failed to read profile data");
                    }
                },
                error -> {
                    int code = error.networkResponse != null ? error.networkResponse.statusCode : -1;
                    if (code == 401 || code == 403) {
                        if (callback != null) callback.onAuthError();
                    } else {
                        if (callback != null) callback.onError(extractMessage(error));
                    }
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
        VolleySingleton.getInstance(context).addToRequestQueue(request);
    }

    /** Latest cached profile (by last sync), or null when never synced. */
    public DriverProfile getCached(Context context) {
        try {
            Box<DriverProfile> box = ((MyApp) context.getApplicationContext()).getBoxStore().boxFor(DriverProfile.class);
            return box.query().orderDesc(DriverProfile_.fetchedAt).build().findFirst();
        } catch (Exception e) {
            return null;
        }
    }

    /** Cached recent trips for the driver, newest first. */
    public List<DriverTrip> getCachedTrips(Context context, long driverId) {
        try {
            Box<DriverTrip> box = ((MyApp) context.getApplicationContext()).getBoxStore().boxFor(DriverTrip.class);
            return box.query().equal(DriverTrip_.driverId, driverId)
                    .orderDesc(DriverTrip_.tripId)
                    .build().find();
        } catch (Exception e) {
            return null;
        }
    }

    private void save(Context context, DriverProfile profile) {
        BoxStore store = ((MyApp) context.getApplicationContext()).getBoxStore();
        Box<DriverProfile> profileBox = store.boxFor(DriverProfile.class);
        for (DriverProfile old : profileBox.query().equal(DriverProfile_.driverId, profile.getDriverId()).build().find()) {
            if (old.id != profile.id) profileBox.remove(old);
        }
        profileBox.put(profile);

        Box<DriverTrip> tripBox = store.boxFor(DriverTrip.class);
        for (DriverTrip old : tripBox.query().equal(DriverTrip_.driverId, profile.getDriverId()).build().find()) {
            tripBox.remove(old);
        }
        if (profile.trips != null) {
            for (DriverTrip trip : profile.trips) {
                trip.setDriverId(profile.getDriverId());
                tripBox.put(trip);
            }
        }
    }

    private String extractMessage(com.android.volley.VolleyError error) {
        if (error.networkResponse != null && error.networkResponse.data != null) {
            try {
                JSONObject err = new JSONObject(new String(error.networkResponse.data, "UTF-8"));
                if (err.has("message")) return err.getString("message");
            } catch (Exception ignored) {}
        }
        return "Network error";
    }

    private String getAuthToken(Context context) {
        try {
            Box<User> box = ((MyApp) context.getApplicationContext()).getBoxStore().boxFor(User.class);
            User u = box.query().equal(User_.isActive, true).build().findFirst();
            return u != null ? u.getToken() : null;
        } catch (Exception e) {
            return null;
        }
    }

    private String nullable(JSONObject o, String key) {
        Object v = o.opt(key);
        return v == null || v == JSONObject.NULL ? null : v.toString();
    }

    private DriverProfile parse(JSONObject root) {
        DriverProfile p = new DriverProfile();

        JSONObject driver = root.optJSONObject("driver");
        if (driver != null) {
            p.setDriverId(driver.optLong("id"));
            p.setDriverName(nullable(driver, "name"));
            p.setDriverEmail(nullable(driver, "email"));
            p.setDriverPhone(nullable(driver, "phone"));
            p.setDriverWhatsapp(nullable(driver, "whatsapp_phone"));
            p.setDriverNationality(nullable(driver, "nationality"));
            p.setDriverBranch(nullable(driver, "branch"));
            p.setRating(driver.optDouble("rating", 0));
            p.setRatingCount(driver.optInt("rating_count", 0));
            p.setMemberSince(nullable(driver, "member_since"));
        }

        JSONObject vehicle = root.optJSONObject("vehicle");
        if (vehicle != null) {
            p.setVehicleId(vehicle.optLong("id"));
            p.setPlateNumber(nullable(vehicle, "plate_number"));
            p.setVehicleMake(nullable(vehicle, "make"));
            p.setVehicleModel(nullable(vehicle, "model"));
            p.setFuelType(nullable(vehicle, "fuel_type"));
            p.setVehicleStatus(nullable(vehicle, "status"));
            JSONObject trailer = vehicle.optJSONObject("trailer");
            if (trailer != null) p.setTrailerPlate(nullable(trailer, "plate_number"));
        }

        JSONObject stats = root.optJSONObject("stats");
        if (stats != null) {
            p.setTotalTrips(stats.optInt("total_trips", 0));
            p.setCompletedTrips(stats.optInt("completed_trips", 0));
            p.setPendingTrips(stats.optInt("pending_trips", 0));
            p.setTotalDistanceKm(stats.optDouble("total_distance_km", 0));
            p.setHoursDriven(stats.optDouble("hours_driven", 0));
        }

        JSONArray trips = root.optJSONArray("latest_trips");
        if (trips != null) {
            java.util.ArrayList<DriverTrip> list = new java.util.ArrayList<>();
            for (int i = 0; i < trips.length(); i++) {
                JSONObject t = trips.optJSONObject(i);
                if (t == null) continue;
                DriverTrip dt = new DriverTrip();
                dt.setTripId(t.optLong("id"));
                dt.setReference(nullable(t, "reference"));
                dt.setStatus(nullable(t, "status"));
                dt.setOrigin(nullable(t, "origin"));
                dt.setDestination(nullable(t, "destination"));
                dt.setEndedAt(nullable(t, "ended_at"));
                list.add(dt);
            }
            p.trips = list;
        }

        p.setRawJson(root.toString());
        return p;
    }
}
