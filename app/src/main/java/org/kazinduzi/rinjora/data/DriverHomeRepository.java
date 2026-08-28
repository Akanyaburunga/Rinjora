package org.kazinduzi.rinjora.data;

import android.content.Context;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.toolbox.JsonObjectRequest;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

import io.objectbox.Box;
import org.kazinduzi.rinjora.MyApp;
import org.kazinduzi.rinjora.entities.HomeSnapshot;
import org.kazinduzi.rinjora.entities.HomeSnapshot_;
import org.kazinduzi.rinjora.entities.User;
import org.kazinduzi.rinjora.entities.User_;
import org.kazinduzi.rinjora.network.ApiConfig;
import org.kazinduzi.rinjora.network.VolleySingleton;

/**
 * Offline-first repository for the driver home dashboard.
 *
 * Every fetch writes a {@link HomeSnapshot} to ObjectBox before the UI reads it, so the
 * home screen renders instantly from cache even fully offline, then refreshes in the
 * background (WorkManager periodic sync + 60s poll while visible).
 *
 * Response handling per docs/ANDROID_DRIVER_HOME.md:
 *   200 -> active trip, full payload
 *   403 -> user has no driver profile (auth error -> back to login)
 *   404 -> driver idle (no active trip) -> idle/empty state, cache preserved
 */
public class DriverHomeRepository {

    private static final String TAG = "DriverHomeRepository";

    public interface Callback {
        void onSuccess(HomeSnapshot snapshot, boolean idle);
        void onAuthError();
        void onError(String message);
    }

    /** GET /api/mobile/trips/current, then cache the result. */
    public void fetch(Context context, Callback callback) {
        String token = getAuthToken(context);
        if (token == null) {
            if (callback != null) callback.onAuthError();
            return;
        }

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.GET, ApiConfig.TRIP_CURRENT, null,
                response -> {
                    try {
                        HomeSnapshot snapshot = parse(response);
                        snapshot.setFetchedAt(System.currentTimeMillis());
                        save(context, snapshot);
                        if (callback != null) callback.onSuccess(snapshot, false);
                    } catch (Exception e) {
                        Log.e(TAG, "Failed to parse home payload", e);
                        if (callback != null) callback.onError("Failed to read home data");
                    }
                },
                error -> {
                    int code = error.networkResponse != null ? error.networkResponse.statusCode : -1;
                    if (code == 401 || code == 403) {
                        if (callback != null) callback.onAuthError();
                    } else if (code == 404) {
                        HomeSnapshot idle = idleSnapshot(context);
                        if (callback != null) callback.onSuccess(idle, true);
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

    /** Latest cached snapshot (by last sync), or null when never synced. */
    public HomeSnapshot getCached(Context context) {
        try {
            Box<HomeSnapshot> box = ((MyApp) context.getApplicationContext()).getBoxStore().boxFor(HomeSnapshot.class);
            return box.query().orderDesc(HomeSnapshot_.fetchedAt).build().findFirst();
        } catch (Exception e) {
            return null;
        }
    }

    /** How old the cached snapshot is, in ms. -1 when there is no cache. */
    public long getStalenessMs(Context context) {
        HomeSnapshot snap = getCached(context);
        return snap != null ? System.currentTimeMillis() - snap.getFetchedAt() : -1;
    }

    private void save(Context context, HomeSnapshot snapshot) {
        Box<HomeSnapshot> box = ((MyApp) context.getApplicationContext()).getBoxStore().boxFor(HomeSnapshot.class);
        for (HomeSnapshot old : box.query().equal(HomeSnapshot_.driverId, snapshot.getDriverId()).build().find()) {
            if (old.id != snapshot.id) box.remove(old);
        }
        box.put(snapshot);
    }

    /** On 404 keep the last known identity (driver/vehicle/staff) but mark the driver idle. */
    private HomeSnapshot idleSnapshot(Context context) {
        HomeSnapshot cached = getCached(context);
        HomeSnapshot idle = cached != null ? cached : new HomeSnapshot();
        idle.setHasActiveTrip(false);
        idle.setFetchedAt(System.currentTimeMillis());
        return idle;
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

    private HomeSnapshot parse(JSONObject root) {
        HomeSnapshot s = new HomeSnapshot();

        JSONObject driver = root.optJSONObject("driver");
        if (driver != null) {
            s.setDriverId(driver.optLong("id"));
            s.setDriverName(nullable(driver, "name"));
            s.setDriverEmail(nullable(driver, "email"));
            s.setDriverPhone(nullable(driver, "phone"));
            s.setDriverWhatsapp(nullable(driver, "whatsapp_phone"));
            s.setDriverNationality(nullable(driver, "nationality"));
            s.setDriverBranch(nullable(driver, "branch"));
        }

        JSONObject vehicle = root.optJSONObject("vehicle");
        if (vehicle != null) {
            s.setVehicleId(vehicle.optLong("id"));
            s.setPlateNumber(nullable(vehicle, "plate_number"));
            s.setVehicleMake(nullable(vehicle, "make"));
            s.setVehicleModel(nullable(vehicle, "model"));
            s.setFuelType(nullable(vehicle, "fuel_type"));
            s.setTankCapacity(vehicle.optDouble("tank_capacity", 0));
            s.setVehicleStatus(nullable(vehicle, "status"));
            JSONObject trailer = vehicle.optJSONObject("trailer");
            if (trailer != null) s.setTrailerPlate(nullable(trailer, "plate_number"));
        }

        JSONObject position = root.optJSONObject("position");
        s.setHasPosition(position != null);
        if (position != null) {
            s.setLatitude(position.optDouble("latitude", 0));
            s.setLongitude(position.optDouble("longitude", 0));
            s.setSpeed(position.optDouble("speed", 0));
            s.setFuelLevel(position.optDouble("fuel_level", 0));
            s.setLastSeenAt(nullable(position, "last_seen_at"));
            s.setMoving(position.optBoolean("is_moving", false));
            s.setIgnition(position.optBoolean("ignition", false));
            s.setStale(position.optBoolean("is_stale", false));
        }

        JSONObject place = root.optJSONObject("nearest_place");
        if (place != null) {
            s.setPlaceId(place.optLong("id"));
            s.setPlaceName(nullable(place, "name"));
            s.setPlaceType(nullable(place, "type"));
            s.setPlaceCity(nullable(place, "city"));
            s.setPlaceDistanceMeters(place.optInt("distance_meters", 0));
            s.setPlaceLat(place.optDouble("latitude", 0));
            s.setPlaceLng(place.optDouble("longitude", 0));
        }

        JSONObject staff = root.optJSONObject("assigned_staff");
        if (staff != null) {
            s.setStaffName(nullable(staff, "name"));
            s.setStaffPhone(nullable(staff, "phone"));
            s.setStaffWhatsapp(nullable(staff, "whatsapp"));
            JSONArray roles = staff.optJSONArray("roles");
            if (roles != null && roles.length() > 0) {
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < roles.length(); i++) {
                    if (i > 0) sb.append(", ");
                    sb.append(roles.optString(i));
                }
                s.setStaffRoles(sb.toString());
            }
        }

        JSONObject trip = root.optJSONObject("trip");
        if (trip != null) {
            s.setTripId(trip.optLong("id"));
            s.setTripReference(nullable(trip, "reference"));
            s.setTripStatus(nullable(trip, "status"));
            JSONObject order = trip.optJSONObject("order");
            if (order != null) {
                s.setOrderReference(nullable(order, "reference"));
                s.setOrderOrigin(nullable(order, "origin"));
                s.setOrderDestination(nullable(order, "destination"));
            }
            JSONObject route = trip.optJSONObject("route");
            if (route != null) {
                s.setRouteName(nullable(route, "name"));
                s.setRouteDistanceKm(route.optDouble("estimated_distance_km", 0));
            }
        }

        s.setHasActiveTrip(true);
        s.setRawJson(root.toString());
        return s;
    }
}
