# Backend Debug Request — `GET /api/mobile/trips/current` (telemetry + active trip)

The Android driver app reports two problems with `GET /api/mobile/trips/current`:

1. **Vehicle telemetry never shows** — no fuel, engine, speed, or GPS signal on the driver home screen.
2. **A trip created on the admin portal for the logged-in driver does not show** — the app keeps saying
   "No active trip right now", even though the trip is visible on the portal.

This doc explains exactly what the app does with your response, what it expects, and a checklist of the
most likely root causes so you can reproduce and fix them.

---

## 1. How the app interprets your response (the contract)

The Android client treats **three** statuses as meaningful. Everything else is a generic network error.

| Status | App behaviour |
|--------|---------------|
| `200` | Driver has an **active trip** → shows trip card + telemetry + position. |
| `403` | `"User is not registered as a driver."` → app treats this as an **auth error** and logs the user out. |
| `404` | `"No active trip found."` → app shows the **idle state** ("No active trip right now"). |

> **This is the key fact for problem 2:** the app only displays a trip when you answer `200`.
> If the trip exists on the portal but the app says "No active trip", your endpoint is answering **`404`**
> (or the app is talking to a different environment — see §4.3).

The app then parses **only** the exact JSON keys below. Any other key is ignored. Use this list as the
ground truth for what must be in the payload.

```json
{
  "driver":        { "id", "name", "email", "phone", "whatsapp_phone", "nationality", "branch" },
  "vehicle":       { "id", "plate_number", "make", "model", "fuel_type", "tank_capacity", "status",
                     "trailer": { "id", "plate_number" } },
  "position":      { "latitude", "longitude", "speed", "fuel_level", "last_seen_at",
                     "is_moving", "ignition", "is_stale" },
  "nearest_place": { "id", "name", "type", "city", "latitude", "longitude", "distance_meters" },
  "assigned_staff":{ "id", "name", "roles": [], "phone", "whatsapp" },
  "trip":          { "id", "reference", "status",
                     "order": { "reference", "origin", "destination" },
                     "route": { "name", "estimated_distance_km" } }
}
```

---

## 2. Problem 1 — telemetry never shows

### How the app decides to render telemetry

The truck status card (fuel / engine / speed / signal) is rendered by `HomeFragment` only when this
condition is true:

```java
boolean hasTruckStatus =
        s.hasPosition              // ← JSON "position" key is present and not null
     || s.tankCapacity > 0         // ← vehicle.tank_capacity > 0
     || s.lastSeenAt != null       // ← position.last_seen_at present
     || s.isStale;                 // ← position.is_stale true
```

The critical field is **`position`**. In `DriverHomeRepository`:

```java
JSONObject position = root.optJSONObject("position");
s.setHasPosition(position != null);   // false when key is null/absent
```

So **if your response contains `"position": null` (or omits it), the app hides the whole telemetry
card** — nothing about fuel, engine, speed or signal is shown.

### Checks to run on the backend

1. **Is the endpoint emitting a `position` object at all?** Reproduce with the driver's bearer token:
   ```bash
   curl -i -H "Authorization: Bearer <driver_token>" \
        http://192.168.100.156:8000/api/mobile/trips/current
   ```
   Compare the actual JSON against the field list above. If `position` is `null`, that alone explains
   the symptom.

2. **Why is `position` null?** The endpoint takes the truck's **latest `vehicle_snapshots` row**.
   Check:
   - Does the vehicle actually have any `vehicle_snapshots` rows? If the GPS/tracker unit has never
     reported, there is no snapshot → `position` is `null`. **A null `position` is correct behaviour
     when there is genuinely no telemetry** — but then confirm the device is wired to the vehicle and
     reporting to `vehicle_snapshots`.
   - Is the **right vehicle** being resolved? The endpoint must resolve the truck from
     `trip.vehicle_id` when set, otherwise the driver's **active** `driver_vehicle_assignments` record
     (latest `start_date`, `end_date IS NULL`). If the resolved vehicle is `null` or the wrong one,
     the snapshot lookup silently produces nothing. Log the resolved `vehicle_id` and check it has
     snapshots.
   - Are you selecting the snapshot with the right ordering/conditions (e.g. filtered by a timestamp
     window, a device flag, or `deleted_at`)? A filter that excludes the latest row will also drop it.

3. **`vehicle.tank_capacity` is `null` in the payload.** In your example response it is `null`, and the
   app turns that into `0.0` (`optDouble(..., 0)`). Consequences in the app:
   - The **fuel bar and "X L of Y L" detail are hidden** (they require `tank_capacity > 0`), even when
     `position.fuel_level` is present.
   - The speed / engine / signal values *would* still show if `position` is present.
   If you want the fuel gauge on the app, return the real `tank_capacity` for the vehicle (fall back to
   the vehicle's configured capacity), not `null`.

4. **Field-name drift.** If your telemetry model has different names (e.g. `fuel`, `gps`, `device`,
   `speed_kmh`, `engine_on`, `gps_time`, `is_stale` is nested elsewhere), the app will parse `0`/`false`
   defaults and the card will look empty. The names **must** match the list in §1 exactly.

5. **`fuel_level` clamping.** The app already clamps negative readings to 0, so a bad `-0.1` reading is
   not the cause of a hidden card.

---

## 3. Problem 2 — portal trip not visible on the app

### How the app decides there is no trip

- `200` → `hasActiveTrip = true`, trip card rendered.
- `404` → `hasActiveTrip = false`, **idle state** ("No active trip right now"), last known identity kept.

The app does **not** call any other endpoint to find a trip, and does not poll the portal. If you can see
the trip on the portal but the app says "No active trip", the endpoint returned **`404`** (or an error the
app swallowed as idle-adjacent). Work down this list.

### Checks to run on the backend

1. **Trip `driver_id` vs the authenticated user's driver profile.** The endpoint resolves the driver
   from the **authenticated token** (`users.id` → `drivers.user_id`), then looks for a non-terminal trip
   where `trips.driver_id = <that driver id>`. Check:
   - Does the portal-created trip actually have `driver_id` set to **that** driver record?
   - Is the logged-in app user linked to a `drivers` row at all? If not, the endpoint returns **403**
     (not 404) — the app would then bounce the user to login, not to "No active trip". If you're seeing
     403, that's a different bug: the portal must create/link the `drivers` row for that user.
   - Is the trip's `driver_id` the same driver the **portal** shows it for? Portal users can be assigned
     at the `users` level while the app matches at the `drivers` level — a mismatch is the classic cause.

2. **Trip status filter.** The query must exclude terminal/draft statuses. If the portal creates the trip
   in a status your `trips/current` filter does not consider "active" (e.g. `draft`, `pending`,
   `scheduled`, `cancelled`, `completed`), the query finds nothing → 404. Log the query and the status
   of the portal trip. Confirm which statuses count as non-terminal and that `trips/current` uses the
   same set the portal uses for "active".

3. **Environment mismatch — verify this first.** The app in debug builds hits:
   ```
   DEV  http://192.168.100.156:8000/api   (BuildConfig.DEBUG = true)
   PROD https://martin-logistics.nova.bi/api
   ```
   If the trip was created on the **PROD portal** but the phone is running a **debug build** (→ DEV
   backend), or vice-versa, the app is querying a database that does not contain the trip. Confirm
   which environment the portal admin actually used and which one the app instance is pointing at.

4. **Multiple trips.** If a driver has several non-terminal trips, confirm the endpoint deterministically
   returns the right one (latest by `created_at`/`started_at`?) and doesn't accidentally `first()` a
   different driver's trip or an older terminal one.

5. **Caching on the app side.** The app renders from a local cache first, then refreshes in the
   background (60s poll while the screen is visible + a 15-minute WorkManager sync). A brand-new trip
   can take up to ~60s to appear after the screen is shown — the user should pull-to-refresh / press the
   refresh button. This is not a backend bug but is worth confirming before chasing the query.

---

## 4. Expected response (abridged, matches the app's parser)

```json
{
  "message": "Active trip found.",
  "driver": {
    "id": 1, "name": "NKUNZIMANA NESTOR", "email": "n@x.com",
    "phone": "+250700000000", "whatsapp_phone": "+250700000000",
    "nationality": "Burundi", "branch": "Bujumbura"
  },
  "vehicle": {
    "id": 87, "plate_number": "RAH428E", "make": null, "model": null,
    "fuel_type": "diesel", "tank_capacity": 500, "status": "active",
    "trailer": { "id": 57, "plate_number": "RL5929" }
  },
  "position": {
    "latitude": -4.0042782, "longitude": 39.5696106, "speed": 0.0,
    "fuel_level": 320.5, "last_seen_at": "2026-08-14T08:50:04.000000Z",
    "is_moving": false, "ignition": false, "is_stale": false
  },
  "nearest_place": {
    "id": 10, "name": "Petrocity Bonje, Mombasa", "type": "fuel_station",
    "city": "Mombasa", "latitude": -4.0026305, "longitude": 39.5686795,
    "distance_meters": 175
  },
  "assigned_staff": {
    "id": 1, "name": "Blaise Nduwimana",
    "roles": ["super_admin", "Dispatcher"], "phone": "+250700112233", "whatsapp": null
  },
  "trip": {
    "id": 321, "reference": "T-2026-00089", "status": "on_route",
    "order": { "reference": "ORD-2026-0001", "origin": "Kigali", "destination": "Mombasa" },
    "route": { "name": "Kigali–Mombasa", "estimated_distance_km": 1723.4 }
  }
}
```

### When there is no active trip (idle)

Return `404` with a message, **or** a `200` with `"position": null` and no `trip` key — either is
handled. Do **not** return `200` with a `trip` that is actually terminal, as the app will render it as
active.

---

## 5. Quick reproduction script (run before changing anything)

```bash
# 1. Login / obtain a token, then:
curl -sS -i \
  -H "Authorization: Bearer <driver_token>" \
  -H "Accept: application/json" \
  http://192.168.100.156:8000/api/mobile/trips/current | head -100
```

1. Confirm the HTTP status (`200` vs `404` vs `403`).
2. Confirm `"position"` is present and not `null`.
3. Confirm `vehicle.tank_capacity` is a real number, not `null`.
4. Confirm `trip.status` is in the active set and `trip.driver_id` matches the authenticated driver.
5. Confirm the DB you just queried is the same DB the portal admin created the trip in.

If all of the above hold, save the raw response body to a file and send it back to the Android developer
so the client can be checked against the real payload.
