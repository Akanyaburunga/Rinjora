# Driver Home API — Android Client Guide

The **Driver Home** endpoint is the single source of truth for the driver's home/dashboard screen. It returns the driver's identity, the truck (and trailer) they are assigned to, their current trip, their live position with the nearest place, the staff member in charge (with contact details) and the current fuel level.

It reuses the existing `GET /api/mobile/trips/current` endpoint, which has been **enriched** — the previous `trip` object is unchanged, so existing clients keep working.

## Authentication

Requires a **Sanctum bearer token**:

```http
Authorization: Bearer <token>
```

## Endpoint

```
GET /api/mobile/trips/current
```

### Responses

| Status | Meaning | When |
|--------|---------|------|
| `200` | Active trip found; full payload below | The driver has an active (non-terminal) trip |
| `403` | `"User is not registered as a driver."` | Logged-in user has no driver profile |
| `404` | `"No active trip found."` | Driver has no active trip — show the **idle/empty state** |

The endpoint is additive: the `trip` key is returned exactly as before (plus the `route`, `dispatcher` and `created_by` relations now eager-loaded).

## Example response

```json
{
  "message": "Active trip found.",
  "driver": {
    "id": 1,
    "user_id": 2,
    "name": "NKUNZIMANA NESTOR",
    "email": "nestor.nkunzimana@gmail.com",
    "phone": "+250783429240",
    "whatsapp_phone": "+250783429240",
    "nationality": "Burundi",
    "branch": null
  },
  "vehicle": {
    "id": 87,
    "plate_number": "RAH428E",
    "make": null,
    "model": null,
    "fuel_type": null,
    "tank_capacity": null,
    "status": "active",
    "trailer": {
      "id": 57,
      "plate_number": "RL5929"
    }
  },
  "position": {
    "latitude": -4.0042782,
    "longitude": 39.5696106,
    "speed": 0.0,
    "fuel_level": -0.1,
    "last_seen_at": "2026-06-20T08:50:04.000000Z",
    "is_moving": false,
    "ignition": false,
    "is_stale": true
  },
  "nearest_place": {
    "id": 10,
    "place_key": "PL-2026-0010",
    "name": "Petrocity Bonje, Mombasa",
    "type": "fuel_station",
    "city": "Mombasa",
    "latitude": -4.0026305,
    "longitude": 39.5686795,
    "radius_meters": 99.0,
    "distance_meters": 175
  },
  "assigned_staff": {
    "id": 1,
    "name": "Blaise Nduwimana",
    "roles": ["super_admin", "Dispatcher"],
    "phone": "+250700112233",
    "whatsapp": null
  },
  "trip": { "...": "unchanged Trip object; see below" }
}
```

## Field reference

### `driver` — who is logged in

| Field | Type | Notes |
|-------|------|-------|
| `id` | int | `drivers.id` |
| `user_id` | int | `users.id` (the authenticated user) |
| `name` | string | `users.name` |
| `email` | string | `users.email` |
| `phone` | string? | `drivers.phone` |
| `whatsapp_phone` | string? | `drivers.whatsapp_phone` — use for the WhatsApp deep link (`https://wa.me/<digits>`) |
| `nationality` | string? | `drivers.nationality` |
| `branch` | string? | branch name (driver branch, falling back to user branch) |

### `vehicle` — assigned truck and trailer

The truck comes from the trip's `vehicle_id` when set, otherwise from the driver's **active** `driver_vehicle_assignments` record (latest `start_date`, `end_date IS NULL`).

| Field | Type | Notes |
|-------|------|-------|
| `id` | int | `vehicles.id` |
| `plate_number` | string | e.g. `RAH428E` |
| `make` / `model` | string? | |
| `fuel_type` | string? | |
| `tank_capacity` | decimal? | litres — use with `position.fuel_level` for the fuel gauge |
| `status` | string | vehicle status (`active`, `maintenance`, …) |
| `trailer` | object? | `null` when no trailer attached |
| `trailer.id` / `trailer.plate_number` | | linked trailer from `trailer_assignments` (`unassigned_at IS NULL`) |

### `position` — live position and fuel

Latest `vehicle_snapshots` row for the truck. **Always render defensively: fields may be `null`/absent when there is no snapshot** (the key is `null`).

| Field | Type | Notes |
|-------|------|-------|
| `latitude` / `longitude` | double? | GPS coordinates |
| `speed` | double | km/h |
| `fuel_level` | double | current fuel in litres — can be negative/`0` if the unit reports a bad reading; do not trust values below `0` |
| `last_seen_at` | datetime? | timestamp of the last GPS report |
| `is_moving` | bool | |
| `ignition` | bool | |
| `is_stale` | bool | **`true` when the snapshot is older than 10 minutes** — show a "signal lost / no GPS" state rather than a live marker |

### `nearest_place` — current surroundings

Same computation as the portal tracker: the nearest `places` row using `ST_Distance_Sphere`. `null` when the truck has no coordinates.

| Field | Type | Notes |
|-------|------|-------|
| `id` | int | `places.id` |
| `place_key` | string | e.g. `PL-2026-0010` |
| `name` | string | display name |
| `type` | string | `fuel_station`, `depot`, `yard`, `checkpoint`, … |
| `city` | string? | |
| `latitude` / `longitude` | double | |
| `radius_meters` | double | geofence radius |
| `distance_meters` | int | straight-line distance to the truck |

### `assigned_staff` — who the driver can call

The trip's **dispatcher**, falling back to the trip **creator**. `null` when neither is set.

| Field | Type | Notes |
|-------|------|-------|
| `id` | int | `users.id` |
| `name` | string | |
| `roles` | string[] | Spatie role names (e.g. `Dispatcher`) |
| `phone` | string? | phone contact from `contacts` (verified preferred) |
| `whatsapp` | string? | whatsapp contact from `contacts` (verified preferred) |

### `trip` — the active trip (unchanged contract)

The original Trip model (now with `order`, `vehicle`, `route`, `dispatcher`, `createdBy` loaded). Key fields:

| Field | Type | Notes |
|-------|------|-------|
| `id` | int | |
| `reference` | string | e.g. `T-2026-00001` |
| `status` | string | `pre_departure`, `assigned`, `on_route`, … |
| `order` | object? | `reference`, `origin`, `destination`, `pickup_date`, `status`, … |
| `route` | object? | `name`, `fleet_key`, `estimated_distance_km`, `path`, … (`null` when no route set) |
| `vehicle` | object? | snapshot of the trip truck (may duplicate the top-level `vehicle`) |

## Recommended client handling

1. **Auth failures** (`401`/`403`) → send the driver back to login.
2. **`404`** → the driver is idle; show an empty state (e.g. "No active trip"). The home screen can still be shown with placeholders.
3. **Position rendering**: use `position.latitude`/`longitude` for the map marker. When `position.is_stale` is true, grey out the marker and show "Last seen <relative time>".
4. **Fuel gauge**: `position.fuel_level` vs `vehicle.tank_capacity`. Clamp `fuel_level` to `>= 0` and show a low-fuel warning below ~15% of capacity.
5. **Call staff**: a call button using `assigned_staff.phone`, and/or a WhatsApp button using `assigned_staff.whatsapp` → `https://wa.me/<digits-only>`.
6. **Nearest place**: show `nearest_place.name` with `nearest_place.distance_meters` formatted (e.g. "175 m"), and optionally deep-link to Google Maps with the place coordinates.
7. **Refresh strategy**: poll this endpoint on an interval while the trip screen is visible (e.g. every 60s) to keep position/fuel/status fresh.
