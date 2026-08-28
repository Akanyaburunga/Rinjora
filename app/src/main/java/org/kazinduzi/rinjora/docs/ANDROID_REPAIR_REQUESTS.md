# Android Repair Requests — Build Guide

A guide for an AI agent implementing the **repair request** feature and its **full lifecycle** on the Android (Kotlin/Java) driver mobile companion app, backed by the Martin Logistics Laravel API.

**Status: all backend endpoints below are implemented and smoke-tested on the server.** No further backend work is required to build the Android client; the agent can build directly against the contracts in this document.

---

## 1. What this feature is

Drivers report vehicle faults at the yard; a workshop team (dispatchers, approvers, mechanics) works the request through inspection, approval, assignment, repair, and release. The Android app has **two audiences**:

| Role | What they do in the app |
|---|---|
| **Driver** | Create a repair request at the yard (with photos + parts), submit it, track status, get notified when the vehicle is released |
| **Mechanic** | See assigned tasks, view task detail, start work, complete work |

### Mobile API summary (all live)

| Route | Method | Who |
|---|---|---|
| `api/mobile/auth/login` | POST | all |
| `api/mobile/auth/request-whatsapp-otp`, `verify-whatsapp-otp`, `verify-firebase-phone`, `logout` | POST | all |
| `api/mobile/repair-requests/vehicles` | GET | driver |
| `api/mobile/repair-requests/parts` | GET | driver |
| `api/mobile/repair-requests/current` | GET | driver |
| `api/mobile/repair-requests` | GET/POST | driver |
| `api/mobile/repair-requests/{id}` | GET | owner / mechanic / admin |
| `api/mobile/repair-requests/{id}/submit` | POST | owner |
| `api/mobile/repair-requests/{id}/cancel` | POST | owner |
| `api/mobile/workshop/my-tasks` | GET | mechanic |
| `api/mobile/workshop/tasks/{assignment}` | GET | mechanic |
| `api/mobile/workshop/tasks/{assignment}/start` | POST | mechanic |
| `api/mobile/workshop/tasks/{assignment}/complete` | POST | mechanic |
| `api/mobile/fcm-token` | POST | all |

---

## 2. Lifecycle / state machine

A `RepairRequest` moves through these statuses:

```
draft ──submit──> pending_approval ──approve(1st, logistics)──> pending_ops_approval ──approve(2nd, ops)──> approved
   │                   │                     └──────reject──────> draft                        │
   │                   │                                                                      │
   │                   └───────── submit without parts ──────────> approved ─────────────────┘
   │                                                                  │
   └──cancel──> cancelled                                         assignMechanic
                                                                      │
                                                                      v
                                                                 in_progress ──completeWork──> completed ──release──> released
```

Key rules (enforced by `RepairRequestService` in `app/Services/RepairRequestService.php`):

1. **Create → `draft`.** Reference auto-generated: `RR-YYYYMMDD-NNNN`.
2. **Submit → `pending_approval`** if any line item has a `part_id`, else straight to **`approved`**.
3. **Approval is two-level** (handled by workshop staff in the web portal, not the app): first level = Logistics Manager → `pending_ops_approval`; second = Operations Manager → `approved`. An `Approval` row is recorded each time.
4. **Assign mechanic → `in_progress`** when the request is `approved`.
5. **Complete work:** an assignment goes `assigned → in_progress → completed`; when *all* assignments complete, the request becomes `completed`.
6. **Release:** all assignments must be completed. Creates a `RepairRelease`, sets the vehicle to `released_from_workshop`, updates `last_odometer`, request → `released`, notifies the driver.
7. **Reject** (either stage) sends the request back to `draft`.

Values: `priority` ∈ `low|medium|high|critical`. `type` is a free string (e.g. `mechanical`, `electrical`, `body`, `tire`, `brake`).

---

## 3. Authentication

All mobile endpoints require a **Sanctum token** as a `Bearer` token.

```http
POST /api/mobile/auth/login
Content-Type: application/json

{
  "email": "driver@example.com",
  "password": "password"
}
```

Response (200):

```json
{
  "message": "Login successful.",
  "token": "1|abc123...",
  "user": { "id": 1, "name": "John Driver", "email": "john@example.com", "roles": ["driver"] }
}
```

Also available:
- `POST /api/mobile/auth/request-whatsapp-otp` — body `{ "identifier": "<whatsapp-number>" }`
- `POST /api/mobile/auth/verify-whatsapp-otp` — body `{ "identifier": "...", "code": "123456" }` → returns `token` + `user`
- `POST /api/mobile/auth/verify-firebase-phone` — body `{ "idToken": "...", "identifier": "<phone-number>" }` → returns `token` + `user`
- `POST /api/mobile/auth/logout` (authenticated)

Store the token in encrypted storage; attach it to every request. On `401`, clear the session and route to login.

> **Note:** `mobile/auth/login` is the login for the mobile client. Do **not** use `/api/login` from `MOBILE_SUPPORT_API.md` (an older, superseded contract).

---

## 4. Driver — repair request endpoints

All under `Authorization: Bearer <token>`.

### 4.1 My vehicles

```
GET /api/mobile/repair-requests/vehicles
```

Vehicles currently assigned to the authenticated driver (via `driver_vehicle_assignments`, `end_date` null).

```json
[
  {
    "id": 87,
    "plate_number": "RAH428E",
    "status": "available",
    "current_driver": "NKUNZIMANA NESTOR",
    "current_driver_id": 2
  }
]
```

### 4.2 Parts catalog (for line items)

```
GET /api/mobile/repair-requests/parts?q=brake
```

Optional `q` searches name/SKU. Returns up to 15:

```json
[
  {
    "id": 41,
    "name": "Brake Pad Set",
    "sku": "BRK-PAD-001",
    "unit_price": 450.0,
    "unit_of_measure": "set"
  }
]
```

If `q` is empty, returns an empty array (the client can show a free-text line item instead).

### 4.3 My current active request

```
GET /api/mobile/repair-requests/current
```

Returns the driver's active request (status not `released`/`cancelled`), or **404** with `{ "message": "No active repair request." }`. Use this on the app home screen to resume an in-flight request.

### 4.4 List my requests

```
GET /api/mobile/repair-requests?status=&priority=&search=&per_page=20
```

Pagination envelope (Laravel default). Returns requests where the user is the `driver_id`, or whose vehicle is currently assigned to them, newest first.

```json
{
  "data": [
    {
      "id": 12,
      "reference": "RR-20260811-0001",
      "vehicle_id": 87,
      "driver_id": 2,
      "mechanic_id": null,
      "type": "brake",
      "priority": "high",
      "description": "Squealing front brakes.",
      "status": "pending_approval",
      "submitted_at": "2026-08-11T09:00:00.000000Z",
      "coordinates": { "lat": -26.2041, "lng": 28.0473 },
      "geofence_verified": true,
      "photo_urls": ["https://example.com/pad.jpg"],
      "created_at": "...", "updated_at": "...",
      "vehicle": { "id": 87, "plate_number": "RAH428E", "status": "available" },
      "items": [
        { "id": 55, "description": "Front brake pads", "part_id": 41, "estimated_quantity": 1, "estimated_unit_price": 450, "estimated_total": 450, "actual_quantity": null, "actual_unit_price": null, "actual_total": null, "part": { "id": 41, "name": "Brake Pad Set", "sku": "BRK-PAD-001" } }
      ],
      "assignments": [ { "id": 5, "status": "assigned", "mechanic": { "id": 7, "name": "M. Ndlovu" } } ]
    }
  ],
  "links": { ... },
  "meta": { ... }
}
```

### 4.5 Create a request

```
POST /api/mobile/repair-requests
Content-Type: application/json
```

```json
{
  "vehicle_id": 87,
  "type": "brake",
  "priority": "high",
  "description": "Squealing front brakes.",
  "latitude": -26.2041,
  "longitude": 28.0473,
  "items": [
    { "description": "Front brake pads", "part_id": 41, "estimated_quantity": 1, "estimated_unit_price": 450 },
    { "description": "Labour", "estimated_quantity": 2, "estimated_unit_price": 80 }
  ],
  "photo_urls": ["https://example.com/pad.jpg"]
}
```

Behavior:
- `driver_id` is **auto-set** to the authenticated user. Do not send it.
- `vehicle_id` must be one of the driver's currently assigned vehicles (**403** otherwise).
- Only drivers (or workshop staff) may call this (**403** otherwise).
- If **both** `latitude` and `longitude` are present, the server runs a **geofence check** against configured yards and returns **422 "Vehicle is not inside the yard..."** when outside. The app should send fresh GPS coords; if a yard is not configured the check still applies, so surface the 422 message verbatim.
- Active-guard: a driver cannot have more than one request not in `released`/`cancelled` (**422** otherwise).
- Line items: `description` required; `part_id` optional (free-text items allowed); `estimated_quantity`/`estimated_unit_price` optional; `estimated_total` is computed server-side.
- `photo_urls` are plain URL strings. There is **no upload endpoint yet** — upload to the app's storage layer and pass the returned URL (see §8).

Response **201** — the created request (status `draft`) with `vehicle` and `items.part`.

### 4.6 Show a request

```
GET /api/mobile/repair-requests/{id}
```

Returns the request with `vehicle`, `mechanic`, `driver`, `items.part`, `approvals`, `assignments.mechanic`, `release.releasedBy`, `purchaseOrders`. **403** unless the user owns it, is a mechanic assigned to it, or is an admin.

### 4.7 Submit

```
POST /api/mobile/repair-requests/{id}/submit
```

Owner only. Moves `draft` → `pending_approval` (items with parts) or → `approved` (no parts). Response: request with `vehicle` + `items`.

### 4.8 Cancel

```
POST /api/mobile/repair-requests/{id}/cancel
```

Owner only. Sets status `cancelled`. Response: request with `vehicle`.

---

## 5. Mechanic — workshop endpoints (also live)

### 5.1 My tasks

```
GET /api/mobile/workshop/my-tasks
```

Plain array (no envelope), ordered `assigned → in_progress → completed`:

```json
[
  {
    "id": 5,
    "repair_request_id": 12,
    "reference": "RR-20260811-0001",
    "vehicle_plate": "RAH428E",
    "instructions": "Inspect front brake pads.",
    "status": "assigned",
    "duration": null,
    "assigned_at": "2026-08-11T09:10:00.000000Z",
    "started_at": null,
    "completed_at": null,
    "completed_note": null
  }
]
```

`duration` is a human string like `"1h 25m"` (null until started).

### 5.2 Task detail

```
GET /api/mobile/workshop/tasks/{assignment}
```

403 unless the assignment belongs to the authenticated mechanic.

```json
{
  "assignment": { "id": 5, "instructions": "...", "status": "in_progress", "duration": "25m", "assigned_at": "...", "started_at": "...", "completed_at": null, "completed_note": null },
  "repair_request": {
    "reference": "RR-20260811-0001",
    "type": "brake",
    "priority": "high",
    "description": "Squealing front brakes.",
    "status": "in_progress",
    "vehicle": { "id": 87, "plate_number": "RAH428E", "status": "in_workshop" },
    "driver": { "id": 2, "name": "NKUNZIMANA NESTOR", "phone": "+27820000000" },
    "items": [
      { "description": "Front brake pads", "part": { "name": "Brake Pad Set", "sku": "BRK-PAD-001" }, "estimated_quantity": 1, "estimated_unit_price": 450 }
    ]
  }
}
```

### 5.3 Start work

```
POST /api/mobile/workshop/tasks/{assignment}/start
```

422 if not currently `assigned`. Response: `{ "message": "Work started.", "assignment": { "id", "status", "started_at" } }`.

### 5.4 Complete work

```
POST /api/mobile/workshop/tasks/{assignment}/complete
Content-Type: application/json

{ "completed_note": "Replaced pads and rotor." }
```

422 if not `in_progress`. Response: `{ "message": "Work completed.", "assignment": { "id", "status", "completed_at" } }`.

### 5.5 FCM token registration

```
POST /api/mobile/fcm-token
{ "token": "<firebase-messaging-token>" }
```

Call after login and on every Firebase `onNewToken` callback.

---

## 6. Push notifications (FCM)

Notifications go through `FcmChannel`. Payloads arrive in the Android `data` map — **all values are strings**, parse ints carefully.

| `type` | Sent to | `data` keys | Navigate to |
|---|---|---|---|
| `mechanic_assigned` | mechanic | `type`, `repair_request_id`, `assignment_id`, `reference` | Task detail (`workshop/tasks/{assignment_id}`) |
| `repair_released` | driver | `type`, `repair_request_id`, `reference` | Repair detail (`repair-requests/{id}`) |
| `repair_created` | driver (portal-created only) | `type`, `repair_request_id`, `reference` | Repair detail |

Register a `FirebaseMessagingService`; in `onMessageReceived` route on `data.type` and launch the matching screen.

---

## 7. Android architecture (recommended)

- **Language:** Kotlin (or Java — the API is stack-agnostic)
- **Networking:** Retrofit + OkHttp + kotlinx.serialization (or Moshi/Gson)
- **Async:** Coroutines / Flow (or RxJava)
- **DI:** Hilt (or Dagger)
- **Storage:** Room for caching lists; DataStore/EncryptedSharedPreferences for the token
- **Images:** capture with camera intent, upload via the app's storage layer, store the returned URL

Package layout:

```
com.martin.logistics/
├── data/
│   ├── remote/        # Retrofit services + DTOs
│   ├── local/         # Room entities, DAOs
│   └── repository/    # RepairRequestRepository, WorkshopRepository, AuthRepository
├── domain/            # models, state machines
├── ui/
│   ├── auth/
│   ├── repairs/       # driver: list, create, detail
│   ├── workshop/      # mechanic: tasks, task detail
│   └── notifications/ # deep-link handling
└── di/
```

---

## 8. Screens to build

### Driver

1. **Repair Request List** (`GET .../repair-requests`) — cards keyed by reference, plate, type badge, priority color, status chip. Tabs: Active / Completed / All. Use `current` (§4.3) to show a "You have an open request" banner.
2. **Create Repair Request** — vehicle picker from `vehicles` (§4.1), type + priority, description, photos, line items (description + optional part + est. qty + est. unit price; part search via §4.2). Send fresh GPS; surface the 422 "not inside the yard" message verbatim. On success navigate to detail.
3. **Repair Request Detail** (`GET .../repair-requests/{id}`) — header (reference, status, priority, vehicle), photos, description, line items (estimated vs actual), approval timeline, assignments, release block. Actions: **Submit** (only `draft`), **Cancel** (draft/pending). Status timeline: Submitted → Approved → In workshop → Completed → Released.
4. **My Tasks (mechanic tab)** — reuse §5.

### Mechanic

1. **My Tasks** (§5.1) — plate, reference, instructions, duration.
2. **Task Detail** (§5.2) — vehicle + driver info, description, parts/items, assignment status. Buttons: **Start Work** (only `assigned`), **Complete Work** (only `in_progress`, optional note).

### Shared

- Push deep links (§6).
- Global 401 interceptor → login.
- Error toasts mapped from §9.

---

## 9. Error handling reference

| Status | Body | Meaning / action |
|---|---|---|
| 401 | `{ "message": "Unauthenticated." }` | Invalid/expired token → re-login |
| 403 | `{ "message": "Vehicle is not assigned to you." }` | Create with a vehicle the driver doesn't own |
| 403 | `{ "message": "This repair request does not belong to you." }` | Not the owner on submit/cancel |
| 403 | `{ "message": "You do not have access to this repair request." }` / `"This task is not assigned to you."` | Not owner/mechanic → hide/disable |
| 403 | `{ "message": "Only drivers and workshop staff can create repair requests." }` | Role gate on create |
| 422 | `{ "message": "Vehicle is not inside the yard. Service requests can only be submitted from within the yard." }` | Geofence failed → prompt to move into yard |
| 422 | `{ "message": "Driver already has an active repair request..." }` | Active-guard → show the existing request (use `current`) |
| 422 | `{ "message": "Task is not in progress." }` / `"Task is not in assignable status."` | Stale state → refetch list |
| 404 | `{ "message": "No active repair request." }` | Nothing active (expected on fresh installs) |
| 404 | `{ "message": "No query results for model..." }` | Deleted/cancelled → refresh list |

Always refetch after a state-changing call rather than guessing the next status.

---

## 10. Known limitations (no backend work needed)

- **No photo upload endpoint** — upload client-side and submit the URL in `photo_urls`.
- **Parts list** returns at most 15 matches; free-text line items are always supported.
- `vehicles` returns the driver's **currently assigned** vehicles only.
- Approval, mechanic assignment, parts consumption (`use-parts`), and release are **portal-only** actions; the app only tracks their effects via the request/assignment state.

---

## 11. Suggested build order (checklist)

1. **Auth:** login + token storage + 401 interceptor.
2. **Driver list + detail** read-only; verify against a seeded driver.
3. **Create flow** incl. photos + line items; verify geofence 422 handling.
4. **Submit / Cancel**; verify status transitions.
5. **Mechanic tasks** list + detail + start + complete.
6. **FCM:** register token; handle the three `data.type` payloads with deep links.
7. **Offline polish:** cache lists in Room; surface server errors verbatim (geofence + active-guard are server-enforced).

## 12. Verification flow (manual / curl)

1. `POST /api/mobile/auth/login` → token.
2. `GET /api/mobile/repair-requests/vehicles` → the driver's vehicles.
3. `POST /api/mobile/repair-requests` with valid yard coordinates → 201 `draft`; with coords far from any yard → 422 geofence; with an unassigned vehicle → 403.
4. `POST .../{id}/submit` → `pending_approval` (parts) or `approved` (no parts).
5. `POST .../{id}/cancel` → `cancelled`.
6. As a mechanic: `GET /api/mobile/workshop/my-tasks` → task appears after the portal assigns; `start` → `in_progress`; `complete` → `completed`.
7. After the portal releases the vehicle, confirm the driver receives a `repair_released` push and the detail shows `status: released` with the release summary.
