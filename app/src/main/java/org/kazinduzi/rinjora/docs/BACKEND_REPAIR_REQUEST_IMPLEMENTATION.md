# Backend Implementation Request — Repair Request Feature (Android Client)

**For:** Backend / Laravel API team
**From:** Android app `MyMartin` (repair-request feature)
**Date:** 2026-08-11
**Status:** Driver-side mobile endpoints required — currently **404**
**Base URL used by the app (debug):** `http://192.168.100.156:8000/api`
**Auth:** all endpoints below require `Authorization: Bearer <token>` (`auth:sanctum`). Token comes from the existing `/mobile/auth/*` flows (already working).

The Android app is fully built against the contracts below. It currently gets **404** on
`GET /api/mobile/repair-requests` because the driver-side routes do not exist in `routes/api.php`
(your mechanic-side `/api/mobile/workshop/*` routes already exist — shapes below must match or the
mechanic screens will mis-render).

---

## 1. Blocking work: driver-side mobile endpoints (MUST BE ADDED)

Add under `Route::prefix('mobile')->middleware('auth:sanctum')` in `routes/api.php`, backed by a new
`Api\Mobile\MobileRepairRequestController`. The `POST` (create) and `GET list/detail` should **delegate
to the existing `RepairRequestController` / `RepairRequestService` logic** so geofence, active-vehicle
guard, reference generation and `driver_id` assignment stay identical to the portal flow.

### 1.1 `GET /api/mobile/repair-requests`
List the authenticated **driver's** requests, newest first.

- The app sends a plain `JsonArrayRequest` → **the response MUST be a bare JSON array** (not wrapped in `data`).
- Each element MUST contain (all snake_case):
  - `id` int
  - `reference` string
  - `type` string — `mechanical | electrical | body | tire | brake | other`
  - `priority` string — `low | medium | high | critical`
  - `status` string — one of: `draft | pending_approval | pending_ops_approval | approved | in_progress | completed | released | cancelled`
  - `description` string
  - `created_at` string (app displays as-is)
  - `vehicle` object → `{ "id": int, "plate_number": string, "status": string }`
- The app filters Active / Completed / All client-side, so server-side filtering is optional.

### 1.2 `GET /api/mobile/repair-requests/vehicles`
Vehicles the authenticated driver is allowed to open a repair request for.

- **MUST be a bare JSON array.** Each element: `{ "id": int, "plate_number": string, "status": string }`
  (modeled on the portal `vehicles()` / `searchVehicles()` methods).

### 1.3 `POST /api/mobile/repair-requests`
Create a new request. **Delegate to the existing `RepairRequestController::store` logic** (geofence +
active-guard + reference generation). When the authenticated user is a driver, auto-set `driver_id`.

Request body (JSON):
```json
{
  "vehicle_id": 12,
  "type": "mechanical",
  "priority": "high",
  "description": "Engine knocking at high revs",
  "latitude": -1.9509,
  "longitude": 30.0614,
  "items": [
    { "description": "Oil filter", "estimated_quantity": 1, "estimated_unit_price": 4500 }
  ]
}
```
- `vehicle_id`, `type`, `priority`, `description` required.
- `latitude` / `longitude` optional (app omits them when no fix is available).
- `items` optional array of `{ description, estimated_quantity, estimated_unit_price }`.

Responses:
- **2xx** — any 2xx is fine; app shows "Repair request created" and leaves the screen. Request should be created in `draft`.
- **422 geofence failure** — CRITICAL: return `422` with JSON `{ "message": "<why>", "errors": { "location": ["..."] } }`. The app shows an alert titled **"Not inside the yard?"** for any 422, so keep the 422 status for geofence rejections.
- **422 validation** — return Laravel default validation shape `{ "message": "...", "errors": { "field": ["..."] } }`; the app displays the first error string.

### 1.4 `GET /api/mobile/repair-requests/{id}`
Full detail for the **owner only** (403 otherwise). App accepts either a bare object or `{ "data": {...} }`.

Expected fields (app parses exactly these — all snake_case):
- Scalars: `id`, `reference`, `type`, `priority`, `status`, `description`, `created_at`, `updated_at`, `latitude` (double), `longitude` (double)
- `vehicle` (nullable) → `{ "id", "plate_number", "status" }`
- `driver` (nullable) → `{ "id", "name", "phone" }`
- `items` (array) → `{ "id", "description", "estimated_quantity", "estimated_unit_price", "estimated_total", "actual_quantity", "actual_unit_price", "part": { "id", "name", "sku" } | null }`
- `approvals` (array) → `{ "id", "stage", "status", "comment": string|null, "created_at", "actor": { "name" } }`
- `assignments` (array) → `{ "id", "status", "instructions": string|null, "assigned_at", "started_at": string|null, "completed_at": string|null, "completed_note": string|null, "mechanic": { "name" } }`
- `release` (object or null) → `{ "id", "checklist_completed": bool, "unresolved_issues": string|null, "odometer_at_release": string|number|null, "released_at" }`

The app renders a 5-step timeline from `status`: Submitted → Approved → In Workshop → Completed → Released.

### 1.5 `POST /api/mobile/repair-requests/{id}/submit`
No request body. Driver submits a draft → `draft` becomes `pending_approval` (or directly `approved`). Any 2xx accepted; errors as `{ "message": string }`.

### 1.6 `POST /api/mobile/repair-requests/{id}/cancel`
No request body. Driver cancels a `draft` / `pending_approval` / `pending_ops_approval` request → `cancelled`. Any 2xx accepted; errors as `{ "message": string }`.

---

## 2. Mechanic-side endpoints (already live — MUST keep these response shapes)

The app expects these **exact** shapes; any drift breaks the mechanic screens.

### 2.1 `GET /api/mobile/workshop/my-tasks`
- **MUST be a bare JSON array.** Each element:
  - `id` int (this is the **assignment** id — the app passes it to the task endpoints below)
  - `repair_request_id` int
  - `reference` string, `vehicle_plate` string, `instructions` string, `status` string
  - `duration` string|null, `assigned_at` string, `started_at` string|null, `completed_at` string|null, `completed_note` string|null
- `status` values understood by the app: `assigned | in_progress | completed` (app sorts assigned → in_progress → completed client-side).

### 2.2 `GET /api/mobile/workshop/tasks/{assignmentId}`
- Response MUST be `{ "data": { "assignment": {...}, "repair_request": {...} } }` (a bare object also works).
- `assignment`: `{ "id", "reference", "instructions", "status", "duration"|null, "assigned_at", "started_at"|null, "completed_at"|null, "completed_note"|null }`
- `repair_request`: `{ "reference", "type", "priority", "description", "status", "vehicle": { "id", "plate_number", "status" }, "driver": { "id", "name", "phone" }, "items": [ { "description", "estimated_quantity", "estimated_unit_price", "estimated_total", "part": { "id", "name", "sku" }|null } ] }`

### 2.3 `POST /api/mobile/workshop/tasks/{assignmentId}/start`
No body → `assigned` → `in_progress`.

### 2.4 `POST /api/mobile/workshop/tasks/{assignmentId}/complete`
Optional body `{ "completed_note": "string" }` → `in_progress` → `completed`.

---

## 3. Cross-cutting requirements

1. **Auth:** all routes behind `auth:sanctum`; return `401` for missing/invalid bearer tokens. The app already sends `Authorization: Bearer <token>`, `Accept: application/json` (and `Content-Type: application/json` on body-carrying POSTs).
2. **Error format (all endpoints):** always JSON `{ "message": "<human-readable string>" }`; include `errors` map for validation failures. The app surfaces the `message` (or first `errors` entry) in a toast/alert.
3. **Driver list/detail ownership:** list filtered to the authenticated driver; detail is 403 for non-owners.
4. **Create:** only drivers (or mechanics assigned to that vehicle) may create; reuse the existing geofence check and reference generator.
5. **Status vocabulary (do not introduce new values):**
   - Request: `draft`, `pending_approval`, `pending_ops_approval`, `approved`, `in_progress`, `completed`, `released`, `cancelled`
   - Assignment/task: `assigned`, `in_progress`, `completed`
6. **Photos:** the app captures photos **locally only** (never uploaded). No photo endpoint is required for this release; `photo_urls` can stay as-is/empty.
7. **Pagination:** not used by the app — return the full list (or a generous `per_page`). Wrapping paginated results in `{ "data": [...], "links": ... }` will **break** the list screens, which parse a bare array. If you must paginate, keep a raw-array mobile serializer.

## 4. Definition of done (Android will pass these)

1. `GET /api/mobile/repair-requests` → 200 bare array (empty array OK when no requests).
2. `POST /api/mobile/repair-requests` with yard coordinates → 2xx `draft`; with coordinates far from any yard → **422** with `message`.
3. `GET /api/mobile/repair-requests/vehicles` → 200 bare array of the driver's vehicles.
4. `GET/POST /{id}`, `POST /{id}/submit`, `POST /{id}/cancel` → 2xx; detail returns all nested objects per §1.4.
5. Mechanic: `GET /api/mobile/workshop/my-tasks` returns the mechanic's assignments; `start` → `in_progress`; `complete` → `completed`; task detail returns `assignment` + `repair_request` per §2.2.
