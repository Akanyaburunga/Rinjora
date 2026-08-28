# Driver Home — Reading & Handling Telemetry + Active Trip (Android Client Guide)

This doc is for the developer working on the Android client. It explains **how to read and handle** the
payload of `GET /api/mobile/trips/current` so vehicle telemetry shows correctly and an active trip is
recognised. Two bugs are being chased right now:

1. Vehicle telemetry (fuel / engine / speed / GPS signal) is not appearing.
2. A trip created on the admin portal is reported as "No active trip".

Read this before touching the code — the parser and UI already implement the contract below, and most
failures are either a backend contract mismatch or a misunderstanding of how the response is gated.

---

## 1. The contract (status codes)

Handled in `DriverHomeRepository.fetch()` → `Callback`:

| HTTP status | Callback | UI behaviour |
|-------------|----------|--------------|
| `200` | `onSuccess(snapshot, false)` | Active trip → render trip card + telemetry |
| `403` | `onAuthError()` | Not a driver → log out to `AuthActivity` |
| `404` | `onSuccess(idle, true)` | Idle → "No active trip right now" |
| other / network / parse | `onError(message)` | Offline banner; error state only if no cache |

`idle` is **true only on 404**. A `200` always renders the trip. So if the portal has a trip but the app
says "No active trip", the server answered **404** (or the app is pointed at a different environment —
see §5.3) — the client logic itself is behaving as designed.

---

## 2. Where the response is parsed

`data/DriverHomeRepository.java` → `parse(JSONObject root)`. It flattens the payload into the
`entities/HomeSnapshot.java` ObjectBox entity. Exact key mapping (all reads use `opt*`, so missing
keys never crash):

| JSON path | HomeSnapshot field |
|-----------|--------------------|
| `driver.*` | `driverName`, `driverEmail`, `driverPhone`, `driverWhatsapp` (`whatsapp_phone`), `driverNationality`, `driverBranch`, `driverId` |
| `vehicle.*` | `vehicleId`, `plateNumber`, `vehicleMake`, `vehicleModel`, `fuelType`, `tankCapacity` (`optDouble(...,0)`), `vehicleStatus`, `trailer.plate_number` → `trailerPlate` |
| `position` (object or null) | **`hasPosition` = (`position != null`)**; then `latitude`, `longitude`, `speed`, `fuelLevel`, `lastSeenAt`, `moving` (`is_moving`), `ignition`, `stale` (`is_stale`) |
| `nearest_place.*` | `placeId`, `placeName`, `placeType`, `placeCity`, `placeDistanceMeters` (`distance_meters`), `placeLat`, `placeLng` |
| `assigned_staff.*` | `staffName`, `staffPhone`, `staffWhatsapp`, `staffRoles` (from `roles[]` joined) |
| `trip.*` | `tripId`, `tripReference`, `tripStatus`; `order.*` → `orderReference/origin/destination`; `route.name`, `route.estimated_distance_km` → `routeName`/`routeDistanceKm` |

After parse: `fetchedAt = now`, `hasActiveTrip = true`, `rawJson = root.toString()` (keep for debugging),
then saved to ObjectBox keyed by `driverId` (old row deleted).

---

## 3. How the UI gates each section — the important part

`ui/home/HomeFragment.java` → `bind(HomeSnapshot s, boolean idle)`. Understand these gates before
"fixing" anything:

```java
// Truck status card (fuel / engine / speed / signal) — ALL hidden when:
boolean hasTruckStatus = s.isHasPosition()          // position object present
        || s.getTankCapacity() > 0                  // vehicle.tank_capacity > 0
        || s.getLastSeenAt() != null                // position.last_seen_at present
        || s.isStale();                             // position.is_stale true
```

- **If the backend sends `"position": null` or omits it, the entire telemetry card is hidden.** That is
  the number-one reason telemetry "doesn't show" — it is not a bug in the UI code.
- **Fuel bar + "X L of Y L" detail** additionally require `tankCapacity > 0`. The backend example
  payload returns `tank_capacity: null` → `optDouble` gives `0.0` → the fuel bar is hidden **even when**
  `position.fuel_level` is present. `fuel_level` is clamped to `>= 0` before display.
- Speed / engine / signal come from `position.speed`, `position.ignition`, `position.is_stale`.
  `is_stale == true` → signal shows "Stale" + "Signal lost · last seen …" (from `lastSeenAt`).

### Trip card

- `idle == true` → `cardTrip` GONE, `cardIdle` VISIBLE, subtitle "No active trip right now."
- `idle == false` → `cardTrip` VISIBLE with `tripReference`, `tripStatus` (humanised), origin → destination,
  and route name/distance when present.

### Other gates

- `nearest_place` card: shown when `placeName != null`.
- Staff card: shown when `staffName != null`.
- `cardTrip` needs `!idle`; the hero subtitle uses `tripReference` when non-null.

---

## 4. How to read & handle the data (recommended rules)

1. **Never trust `position` to exist.** Render telemetry defensively: if `hasPosition` is false and
   `tankCapacity <= 0` and `lastSeenAt == null`, hide the whole truck-status card (already done). Do not
   show fake `0 km/h` / `0 L` values when the server genuinely has no snapshot.
2. **Don't show a live marker on a stale snapshot.** When `isStale` is true, grey it out and show
   "Signal lost · last seen <relative>" via `DateUtils.relative(lastSeenAt)` (already done).
3. **Fuel gauge math:** `percent = fuel_level / tank_capacity * 100`, clamped to `[0, 100]`; warn below
   `15%` (`LOW_FUEL_THRESHOLD_PERCENT`). `tank_capacity <= 0` → hide the bar (can't compute a percent).
4. **Trip presence = HTTP 200.** The app does not fetch the portal. If the portal shows a trip but the
   endpoint returns 404, surface that to the backend dev (it's a backend data/status problem), not a
   client fix.
5. **Keep the offline-first flow:** render from cache first (`getCached()`), then refresh (60s poll while
   visible + `HomeSyncWorker` every 15 min). After the backend is fixed, a user on the home screen sees
   the new trip within ≤60s; a fresh app launch may take a moment for the first background fetch.

---

## 5. Debugging your own install

### 5.1 Capture the real payload

The repository stores the last successful response in `HomeSnapshot.rawJson`. Add a temporary log in
`parse()` (or read the entity) to dump it:

```java
Log.d("TRIP_CURRENT", "status=200 raw=" + root);
```

### 5.2 Confirm which environment you're hitting

`network/ApiConfig.java`:

```java
BASE_URL = BuildConfig.DEBUG ? "http://192.168.100.156:8000/api"
                             : "https://martin-logistics.nova.bi/api";
```

A **debug build hits the DEV server**. If the portal admin created the trip on **PROD** (or vice-versa),
you are querying a different database — this alone produces "No active trip" with a correct 404.
Check the Network inspector / `Log.d("TRIP_CURRENT", ...)` for the full URL.

### 5.3 Common "it doesn't show" checklist (client side)

- [ ] `getAuthToken()` returned the token (check `User_` box: `isActive == true`, token non-empty).
- [ ] The request actually reached the endpoint (Volley error? CORS? wrong IP? `network_security_config`
      allows cleartext `http://192.168.x.x`?).
- [ ] HTTP status logged is `200` and `position` is non-null in `rawJson`.
- [ ] `tank_capacity` in the response is a real number if you expect the fuel bar.
- [ ] You're on the same network as `192.168.100.156` (dev URL is a LAN address — it fails off-VPN/off-LAN).
- [ ] You force-refreshed (`btnRefresh`); the 60s poll won't fire while the screen is paused.

### 5.4 Server returns 200 but you still see "No active trip"

Then `parse()` threw (check `Log.e("DriverHomeRepository", ...)`). The likely cause is a JSON shape that
`parse()` doesn't expect (e.g. `trip` is an array, or `roles` is not an array). Fix the parse to match the
backend, or log the raw JSON and send it to the backend dev.

---

## 6. Quick reference: what a healthy response produces

| Payload | UI |
|---------|----|
| `position` present, `is_stale:false` | Telemetry card: fuel bar (if `tank_capacity>0`), engine On/Off, speed, signal **Live** |
| `position` present, `is_stale:true` | Telemetry card: signal **Stale** + "Signal lost · last seen …" |
| `position` absent / `null` | **Whole telemetry card hidden** — verify backend, not the UI |
| `trip` + status `200` | Trip card with ref, status, origin → destination, route |
| `404` | Idle state "No active trip right now" |
| `403` | Logged out (not a driver profile) |
