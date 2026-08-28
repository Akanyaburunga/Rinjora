# Backend Implementation Request — Driver Profile Endpoint

Mirrors the `GET /api/mobile/trips/current` enrichment already shipped: build a **driver profile**
endpoint the Android app reads for the Profile screen, in the same additive style.

## Endpoint

```
GET /api/mobile/profile
```

Auth: **Sanctum bearer token** (same as all `/api/mobile/*` routes).

### Responses

| Status | Meaning | When |
|--------|---------|------|
| `200` | Profile found; payload below | Authenticated user has a driver profile |
| `403` | `{ "message": "User is not registered as a driver." }` | Logged-in user has no driver profile |
| `401` | `{ "message": "Unauthenticated." }` | Missing/expired token |

### Example response

```json
{
  "message": "Driver profile.",
  "driver": {
    "id": 1,
    "user_id": 2,
    "name": "NKUNZIMANA NESTOR",
    "email": "nestor.nkunzimana@gmail.com",
    "phone": "+250783429240",
    "whatsapp_phone": "+250783429240",
    "nationality": "Burundi",
    "branch": "Bujumbura",
    "rating": 4.7,
    "rating_count": 43,
    "member_since": "2023-05-01"
  },
  "vehicle": {
    "id": 87,
    "plate_number": "RAH428E",
    "make": null,
    "model": null,
    "fuel_type": "diesel",
    "status": "active",
    "trailer": {
      "id": 57,
      "plate_number": "RL5929"
    }
  },
  "stats": {
    "total_trips": 214,
    "completed_trips": 198,
    "pending_trips": 16,
    "total_distance_km": 48230.5,
    "hours_driven": 1024.0
  },
  "latest_trips": [
    {
      "id": 321,
      "reference": "T-2026-00089",
      "status": "completed",
      "origin": "Kigali",
      "destination": "Mombasa",
      "ended_at": "2026-07-30T18:00:00.000000Z"
    }
  ]
}
```

## Field reference

### `driver`

| Field | Type | Notes |
|-------|------|-------|
| `id` | int | `drivers.id` |
| `user_id` | int | `users.id` (the authenticated user) |
| `name` | string | `users.name` |
| `email` | string | `users.email` |
| `phone` | string? | `drivers.phone` |
| `whatsapp_phone` | string? | `drivers.whatsapp_phone` — app deep-links `https://wa.me/<digits-only>` |
| `nationality` | string? | `drivers.nationality` |
| `branch` | string? | branch name (driver branch, falling back to user branch) |
| `rating` | float? | average rating (`0` when no ratings) |
| `rating_count` | int | number of ratings behind `rating` |
| `member_since` | date? | driver `created_at` date |

### `vehicle`

Same shape as the home endpoint: the trip's truck when set, otherwise the driver's active
`driver_vehicle_assignments` record. Includes `trailer` (`null` when none attached).

### `stats`

| Field | Type | Notes |
|-------|------|-------|
| `total_trips` | int | all trips where the user is `driver_id` |
| `completed_trips` | int | trips in a terminal success state |
| `pending_trips` | int | non-terminal trips |
| `total_distance_km` | float | summed `estimated_distance_km` (or tracked distance if available) |
| `hours_driven` | float | summed tracked hours (may be `0` when untracked) |

### `latest_trips`

Array of the driver's 5 most recent trips by `ended_at` desc (fall back to `created_at` desc).
Each item: `id`, `reference`, `status`, `origin`, `destination`, `ended_at` (ISO datetime?).
If the driver has no trips, return `[]` (never omit the key).

## Implementation notes

1. Reuse the existing driver-resolution helper from `trips/current` (403 when no driver profile).
2. `rating`/`rating_count` come from the driver's ratings relation (the same source the portal
   profile shows).
3. Keep the response **additive**: the `driver` and `vehicle` shapes must stay byte-compatible
   with `trips/current` so the app can share parsing.
4. Register the route inside the `auth:sanctum` middleware group alongside the other `/api/mobile/*`
   routes.
